package com.morphengine.nexus.level;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.network.NetworkNode;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.NetworkBlock;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.energy.EnergyPool;
import com.morphengine.nexus.energy.EnergyRateMeter;
import com.morphengine.nexus.energy.PriorityOrder;
import com.morphengine.nexus.energy.StorageEnergyBuffer;
import com.morphengine.nexus.network.NetworkGraphs;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live state of one network on the server: which blocks belong to it, the energy
 * pool they form, the status its Nexus shows, and the figures shown in the Nexus
 * interface.
 *
 * <p>The energy pool is every Energy Cell at its own priority and the energy
 * in the cells of the network's storage at the priorities of the storages
 * holding it, ranked together by {@link PriorityOrder}. It is put together
 * again after a rebuild and whenever the storage's sources change.
 *
 * <p>A network is led by one Nexus. When a traversal finds another Nexus that
 * {@linkplain #leads leads}, this one stands down: it keeps no members and
 * shows {@link NexusStatus#CONFLICT} until the other one is gone.
 *
 * <p>Membership is rebuilt only after {@link #invalidate()}, on the next tick, or
 * while the last traversal stopped at an unloaded chunk, then once every
 * {@value #RETRY_INTERVAL_TICKS} ticks until the chunk is loaded. Statistics and
 * status are refreshed every {@value #STATISTICS_INTERVAL_TICKS} ticks. A tick
 * without either does not allocate. Server thread only.
 */
public final class NetworkState {

    static final int STATISTICS_INTERVAL_TICKS = 20;
    static final int RETRY_INTERVAL_TICKS = 20;

    private final NetworkController controller;
    private final Map<NetworkComponentType<?>, NetworkComponent> components = createComponents();
    private final EnergyRateMeter meter = new EnergyRateMeter();
    private final NetworkStorage storage;
    private final IEnergyStorage energyHandler = new NetworkEnergyHandler(this);
    private boolean stale = true;
    private boolean partial;
    private boolean conflict;
    private Membership membership = Membership.EMPTY;
    private EnergyPool energy = EnergyPool.EMPTY;
    private PriorityOrder<IEnergyStorage> energyHandlers = PriorityOrder.empty();
    private int poolRevision;
    private NetworkStatistics statistics = NetworkStatistics.EMPTY;
    private NexusStatus status = NexusStatus.NO_ENERGY;

    /**
     * @param controller the Nexus this state belongs to, told to members on join
     *                   and leave; its gate guards the network's storage, asked
     *                   only once the state is in use
     */
    public NetworkState(final NetworkController controller) {
        this.controller = controller;
        this.storage = component(NetworkComponentTypes.STORAGE).storage();
        storage.addInterceptor(component(NetworkComponentTypes.AUTOCRAFTING));
        storage.guardWith((actor, permission) -> controller.gate().permits(actor, permission));
    }

    public void invalidate() {
        stale = true;
    }

    public NetworkStatistics statistics() {
        return statistics;
    }

    public EnergyBuffer energy() {
        return energy;
    }

    /**
     * @return the network's energy as other mods reach it through the Nexus;
     *         the same instance for the whole life of this state
     */
    public IEnergyStorage energyHandler() {
        return energyHandler;
    }

    /**
     * @return transaction-aware handlers of the pool's buffers, ranked as the pool ranks them
     */
    PriorityOrder<IEnergyStorage> energyHandlers() {
        return energyHandlers;
    }

    /**
     * @return this network's component of {@code type}
     */
    public <C extends NetworkComponent> C component(final NetworkComponentType<C> type) {
        final NetworkComponent component = components.get(type);
        if (component == null) {
            throw new IllegalArgumentException("network component type is not registered: " + type);
        }
        @SuppressWarnings("unchecked")
        final C typed = (C) component;
        return typed;
    }

    /**
     * @param origin position of the Nexus that defines this network
     */
    public void tick(final Level level, final BlockPos origin) {
        final long gameTime = level.getGameTime();
        if (stale || partial && gameTime % RETRY_INTERVAL_TICKS == 0) {
            rebuild(level, origin);
        } else if (storage.revision() != poolRevision) {
            assemblePool();
        }
        if (gameTime % STATISTICS_INTERVAL_TICKS == 0) {
            refreshStatistics();
            component(NetworkComponentTypes.ENERGY_ACCOUNT).sample(gameTime);
            refreshStatus(level);
        }
    }

    /**
     * Drops every member from the network, for when the Nexus leaves the level.
     */
    public void release() {
        adopt(Membership.EMPTY);
        stale = true;
    }

    /**
     * Whether {@code first} leads a network it shares with {@code second}: the
     * one established earlier, and on a tie the one with the lower position.
     */
    static boolean leads(final NetworkController first, final NetworkController second) {
        if (first.establishedAt() != second.establishedAt()) {
            return first.establishedAt() < second.establishedAt();
        }
        return first.getBlockPos().asLong() < second.getBlockPos().asLong();
    }

    private void rebuild(final Level level, final BlockPos origin) {
        final MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        final ServerConnections connections = new ServerConnections(server);
        final BlockNode controllerNode = BlockNode.of(level, origin);
        final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(controllerNode, connections);
        partial = connections.reachedUnloaded();
        stale = false;
        conflict = ledByAnother(connections, reachable, controllerNode);
        adopt(conflict ? Membership.EMPTY : Membership.scan(connections, reachable, controllerNode));
        if (!conflict) {
            paintDevices(level, controller.network().color());
        }
        refreshStatus(level);
        showPower(level, status == NexusStatus.ONLINE);
    }

    private boolean ledByAnother(
            final ServerConnections connections, final Set<NetworkNode> reachable, final BlockNode controllerNode) {
        for (NetworkNode node : reachable) {
            if (node instanceof BlockNode blockNode && !node.equals(controllerNode)
                    && outranksThis(blockEntityAt(connections.levelOf(blockNode), blockNode))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable BlockEntity blockEntityAt(final @Nullable Level level, final BlockNode node) {
        return level != null ? level.getBlockEntity(node.position().pos()) : null;
    }

    private boolean outranksThis(final @Nullable BlockEntity blockEntity) {
        return blockEntity instanceof NetworkController other && !other.isRemoved() && leads(other, controller);
    }

    /**
     * Shows {@code color} on the Nexus and every device found by the last
     * rebuild. Touches only devices whose color actually changes. A Nexus that
     * stands down paints nothing.
     */
    public void paintDevices(final Level level, final NetworkColor color) {
        final MinecraftServer server = level.getServer();
        final DyeColor dye = NetworkColoring.dyeOf(color);
        final List<GlobalPos> devices = membership.devices();
        for (int i = 0; i < devices.size() && server != null; i++) {
            final ServerLevel at = loadedLevel(server, devices.get(i));
            if (at != null) {
                NetworkColoring.paint(at, devices.get(i).pos(), dye);
            }
        }
    }

    /**
     * @return the level {@code position} stands in, when it is loaded there; {@code null} otherwise
     */
    private static @Nullable ServerLevel loadedLevel(final MinecraftServer server, final GlobalPos position) {
        final ServerLevel level = server.getLevel(position.dimension());
        return level != null && level.isLoaded(position.pos()) ? level : null;
    }

    private void adopt(final Membership found) {
        for (NetworkMember previous : membership.members()) {
            if (!found.members().contains(previous)) {
                previous.leaveNetwork(controller);
            }
        }
        for (NetworkMember member : found.members()) {
            member.joinNetwork(controller);
        }
        membership = found;
        for (NetworkComponent component : components.values()) {
            component.adopt(found.members());
        }
        assemblePool();
    }

    /**
     * Ranks the Energy Cells found by the last rebuild and the storage's energy,
     * one part per priority of its sources, into the pool.
     */
    private void assemblePool() {
        final List<PriorityOrder.Ranked<EnergyBuffer>> buffers = new ArrayList<>();
        final List<PriorityOrder.Ranked<IEnergyStorage>> handlers = new ArrayList<>();
        for (EnergyContributor contributor : membership.contributors()) {
            final int priority = contributor.energyPriority();
            buffers.add(new PriorityOrder.Ranked<>(contributor.energyBuffer(), priority));
            handlers.add(new PriorityOrder.Ranked<>(contributor.energyHandler(), priority));
        }
        for (int priority : storage.priorities()) {
            final EnergyBuffer band = new StorageEnergyBuffer(storage.band(priority), EnergyKey.INSTANCE,
                    Actor.NOBODY);
            buffers.add(new PriorityOrder.Ranked<>(band, priority));
            handlers.add(new PriorityOrder.Ranked<>(new StoredEnergyHandler(band), priority));
        }
        energy = new EnergyPool(PriorityOrder.of(buffers));
        energyHandlers = PriorityOrder.of(handlers);
        poolRevision = storage.revision();
        meter.rebase();
    }

    private static Map<NetworkComponentType<?>, NetworkComponent> createComponents() {
        final Map<NetworkComponentType<?>, NetworkComponent> created = new IdentityHashMap<>();
        for (NetworkComponentType<?> type : NetworkComponentTypes.ALL) {
            created.put(type, type.create());
        }
        return created;
    }

    private void refreshStatistics() {
        final List<EnergyContributor> contributors = membership.contributors();
        for (int i = 0; i < contributors.size(); i++) {
            if (contributors.get(i).isRemoved()) {
                stale = true;
            }
        }
        final List<NetworkMember> members = membership.members();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isRemoved()) {
                stale = true;
            }
        }
        meter.sample(energy, STATISTICS_INTERVAL_TICKS);
        statistics = new NetworkStatistics(membership.deviceCount(), membership.roles(), energy.stored(),
                energy.capacity(),
                meter.inputPerTick(), meter.outputPerTick());
    }

    private void refreshStatus(final Level level) {
        final NexusStatus current = conflict ? NexusStatus.CONFLICT
                : energy.stored() > 0 ? NexusStatus.ONLINE : NexusStatus.NO_ENERGY;
        if (current != status) {
            final boolean powerChanged = (current == NexusStatus.ONLINE) != (status == NexusStatus.ONLINE);
            status = current;
            if (powerChanged && !conflict) {
                showPower(level, current == NexusStatus.ONLINE);
            }
        }
        controller.showStatus(current);
    }

    private void showPower(final Level level, final boolean powered) {
        final MinecraftServer server = level.getServer();
        final List<GlobalPos> cables = membership.cables();
        for (int i = 0; i < cables.size() && server != null; i++) {
            final ServerLevel at = loadedLevel(server, cables.get(i));
            if (at != null) {
                CableBlock.showPower(at, cables.get(i).pos(), powered);
            }
        }
    }

    /**
     * What one traversal found: the members told about the network, the Energy
     * Cells of its pool, and the positions of its devices and cables.
     *
     * @param devices     devices to paint, the Nexus itself included
     * @param deviceCount devices shown in the Nexus interface, the Nexus itself excluded
     * @param roles       the same devices by what they do
     */
    private record Membership(
            List<NetworkMember> members,
            List<EnergyContributor> contributors,
            List<GlobalPos> devices,
            List<GlobalPos> cables,
            int deviceCount,
            Map<DeviceRole, Integer> roles) {

        static final Membership EMPTY =
                new Membership(List.of(), List.of(), List.of(), List.of(), 0, Map.of());

        static Membership scan(
                final ServerConnections connections, final Set<NetworkNode> reachable,
                final BlockNode controllerNode) {
            final List<NetworkMember> members = new ArrayList<>();
            final List<EnergyContributor> contributors = new ArrayList<>();
            final List<GlobalPos> devices = new ArrayList<>();
            final List<GlobalPos> cables = new ArrayList<>();
            final Map<DeviceRole, Integer> roles = new EnumMap<>(DeviceRole.class);
            for (NetworkNode node : reachable) {
                if (!(node instanceof BlockNode blockNode) || node.equals(controllerNode)) {
                    continue;
                }
                final Level level = connections.levelOf(blockNode);
                final GlobalPos position = blockNode.position();
                final BlockEntity blockEntity = level != null ? level.getBlockEntity(position.pos()) : null;
                if (level == null || blockEntity instanceof NetworkController) {
                    continue;
                }
                if (level.getBlockState(position.pos()).getBlock() instanceof NetworkBlock block) {
                    (block.isDevice() ? devices : cables).add(position);
                    if (block.isDevice()) {
                        roles.merge(block.role(), 1, Integer::sum);
                    }
                }
                if (blockEntity instanceof EnergyContributor contributor) {
                    contributors.add(contributor);
                }
                if (blockEntity instanceof NetworkMember member) {
                    members.add(member);
                }
            }
            final int deviceCount = devices.size();
            devices.add(controllerNode.position());
            return new Membership(List.copyOf(members), List.copyOf(contributors), List.copyOf(devices),
                    List.copyOf(cables), deviceCount, Map.copyOf(roles));
        }
    }
}
