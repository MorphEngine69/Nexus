package com.morphengine.nexus.level;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.network.NetworkNode;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.NetworkBlock;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.energy.EnergyPool;
import com.morphengine.nexus.energy.EnergyRateMeter;
import com.morphengine.nexus.network.NetworkGraphs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Live state of one network on the server: which blocks belong to it, the energy
 * pool they form, the status its Nexus shows, and the figures shown in the Nexus
 * interface.
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
    private final EnergyRateMeter meter = new EnergyRateMeter();
    private boolean stale = true;
    private boolean partial;
    private boolean conflict;
    private Membership membership = Membership.EMPTY;
    private EnergyPool energy = EnergyPool.EMPTY;
    private NetworkStatistics statistics = NetworkStatistics.EMPTY;
    private NexusStatus status = NexusStatus.NO_ENERGY;

    /**
     * @param controller the Nexus this state belongs to, told to members on join and leave
     */
    public NetworkState(final NetworkController controller) {
        this.controller = controller;
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
     * @param origin position of the Nexus that defines this network
     */
    public void tick(final Level level, final BlockPos origin) {
        final long gameTime = level.getGameTime();
        if (stale || partial && gameTime % RETRY_INTERVAL_TICKS == 0) {
            rebuild(level, origin);
        }
        if (gameTime % STATISTICS_INTERVAL_TICKS == 0) {
            refreshStatistics();
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
        final AdjacencyConnections connections = new AdjacencyConnections(level);
        final BlockNode controllerNode = BlockNode.of(level, origin);
        final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(controllerNode, connections);
        partial = connections.reachedUnloaded();
        stale = false;
        meter.rebase();
        conflict = ledByAnother(level, reachable, controllerNode);
        adopt(conflict ? Membership.EMPTY : Membership.scan(level, reachable, controllerNode, origin));
        if (!conflict) {
            paintDevices(level, controller.network().color());
        }
        refreshStatus(level);
        showPower(level, status == NexusStatus.ONLINE);
    }

    private boolean ledByAnother(final Level level, final Set<NetworkNode> reachable, final BlockNode controllerNode) {
        for (NetworkNode node : reachable) {
            if (node instanceof BlockNode blockNode && !node.equals(controllerNode)
                    && outranksThis(level.getBlockEntity(blockNode.position().pos()))) {
                return true;
            }
        }
        return false;
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
        final DyeColor dye = NetworkColoring.dyeOf(color);
        final List<BlockPos> devices = membership.devices();
        for (int i = 0; i < devices.size(); i++) {
            NetworkColoring.paint(level, devices.get(i), dye);
        }
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
        energy = new EnergyPool(found.buffers());
    }

    private void refreshStatistics() {
        final List<EnergyContributor> contributors = membership.contributors();
        for (int i = 0; i < contributors.size(); i++) {
            if (contributors.get(i).isRemoved()) {
                stale = true;
            }
        }
        meter.sample(energy, STATISTICS_INTERVAL_TICKS);
        statistics = new NetworkStatistics(membership.deviceCount(), energy.stored(), energy.capacity(),
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
        final List<BlockPos> cables = membership.cables();
        for (int i = 0; i < cables.size(); i++) {
            CableBlock.showPower(level, cables.get(i), powered);
        }
    }

    /**
     * What one traversal found: the members told about the network, the energy
     * buffers of its pool, and the positions of its devices and cables.
     *
     * @param devices     devices to paint, the Nexus itself included
     * @param deviceCount devices shown in the Nexus interface, the Nexus itself excluded
     */
    private record Membership(
            List<NetworkMember> members,
            List<EnergyContributor> contributors,
            List<EnergyBuffer> buffers,
            List<BlockPos> devices,
            List<BlockPos> cables,
            int deviceCount) {

        static final Membership EMPTY = new Membership(List.of(), List.of(), List.of(), List.of(), List.of(), 0);

        static Membership scan(
                final Level level, final Set<NetworkNode> reachable, final BlockNode controllerNode,
                final BlockPos origin) {
            final List<NetworkMember> members = new ArrayList<>();
            final List<EnergyContributor> contributors = new ArrayList<>();
            final List<EnergyBuffer> buffers = new ArrayList<>();
            final List<BlockPos> devices = new ArrayList<>();
            final List<BlockPos> cables = new ArrayList<>();
            for (NetworkNode node : reachable) {
                if (!(node instanceof BlockNode blockNode) || node.equals(controllerNode)) {
                    continue;
                }
                final BlockPos pos = blockNode.position().pos();
                final BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof NetworkController) {
                    continue;
                }
                if (level.getBlockState(pos).getBlock() instanceof NetworkBlock block) {
                    (block.isDevice() ? devices : cables).add(pos);
                }
                if (blockEntity instanceof EnergyContributor contributor) {
                    contributors.add(contributor);
                    buffers.add(contributor.energyBuffer());
                }
                if (blockEntity instanceof NetworkMember member) {
                    members.add(member);
                }
            }
            final int deviceCount = devices.size();
            devices.add(origin);
            return new Membership(List.copyOf(members), List.copyOf(contributors), List.copyOf(buffers),
                    List.copyOf(devices), List.copyOf(cables), deviceCount);
        }
    }
}
