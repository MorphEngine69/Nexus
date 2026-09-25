package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.block.CoalGeneratorBlock;
import com.morphengine.nexus.energy.FuelBurner;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkLink;
import com.morphengine.nexus.level.NetworkMember;
import com.morphengine.nexus.menu.CoalGeneratorMenu;
import com.morphengine.nexus.menu.CoalGeneratorView;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Burns fuel into its own buffer and passes the energy on: first into the energy
 * pool of its network, then into neighbouring blocks that accept RF. It never
 * draws energy from the network.
 */
public final class CoalGeneratorBlockEntity extends BlockEntity implements MenuHost, NetworkMember, Renamable {

    /** Placeholder balance until the numbers are settled. */
    private static final long ENERGY_PER_TICK = 40;
    private static final long CAPACITY = 100_000;
    private static final long MAX_OUTPUT_PER_SIDE = 1_000;

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_BURN_LEFT = "burn_left";
    private static final String TAG_BURN_TOTAL = "burn_total";
    private static final Direction[] SIDES = Direction.values();

    private final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(CAPACITY, ENERGY_PER_TICK, MAX_OUTPUT_PER_SIDE);
    private final FuelBurner burner = new FuelBurner(ENERGY_PER_TICK);
    private final SimpleContainer fuel = new FuelContainer();
    private final EnergyHandler handler =
            new BufferEnergyHandler(buffer, BufferEnergyHandler.Access.GIVE_ONLY, this::setChanged);
    private final ClickGuard clickGuard = new ClickGuard();
    private final NetworkLink network = new NetworkLink();
    private final DeviceName name = new DeviceName();
    /** Energy handlers of the six neighbours, looked up once and kept current by NeoForge. */
    private List<BlockCapabilityCache<EnergyHandler, Direction>> outputs = List.of();
    private long producedLastTick;

    public CoalGeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.COAL_GENERATOR.get(), pos, state);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final CoalGeneratorBlockEntity generator) {
        if (level instanceof ServerLevel serverLevel) {
            generator.tick(serverLevel, pos, state);
        }
    }

    public static boolean isFuel(final ItemStack stack) {
        return stack.is(NexusTags.COAL_GENERATOR_FUELS);
    }

    public Container fuel() {
        return fuel;
    }

    public EnergyHandler energyHandler() {
        return handler;
    }

    public CoalGeneratorView view() {
        return new CoalGeneratorView(buffer.stored(), buffer.capacity(), producedLastTick,
                burner.burnTicksLeft(), burner.burnTicksTotal(), NetworkBadge.of(network));
    }

    @Override
    public void joinNetwork(final NetworkController joined) {
        network.join(joined);
    }

    @Override
    public void leaveNetwork(final NetworkController left) {
        network.leave(left);
    }

    private void tick(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (!burner.isBurning() && burner.hasRoomIn(buffer)) {
            igniteNextFuel(level);
        }
        producedLastTick = burner.tick(buffer);
        feedNetwork();
        pushToNeighbours(level, pos);
        final boolean lit = producedLastTick > 0;
        if (state.getValue(CoalGeneratorBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CoalGeneratorBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (producedLastTick > 0) {
            setChanged();
        }
    }

    private void igniteNextFuel(final ServerLevel level) {
        final ItemStack stack = fuel.getItem(0);
        if (!isFuel(stack)) {
            return;
        }
        final int burnTicks = level.fuelValues().burnDuration(stack);
        if (burnTicks > 0) {
            stack.shrink(1);
            burner.ignite(burnTicks);
            setChanged();
        }
    }

    private void feedNetwork() {
        final NetworkController controller = network.controller();
        if (controller == null || buffer.stored() == 0) {
            return;
        }
        final long offered = buffer.extract(MAX_OUTPUT_PER_SIDE, Action.SIMULATE);
        final long accepted = controller.energy().insert(offered, Action.EXECUTE);
        if (accepted > 0) {
            buffer.extract(accepted, Action.EXECUTE);
            setChanged();
        }
    }

    private void pushToNeighbours(final ServerLevel level, final BlockPos pos) {
        if (buffer.stored() == 0) {
            return;
        }
        final List<BlockCapabilityCache<EnergyHandler, Direction>> neighbours = outputs(level, pos);
        for (int i = 0; i < neighbours.size(); i++) {
            final EnergyHandler target = neighbours.get(i).getCapability();
            if (target != null) {
                pushTo(target);
            }
        }
    }

    private void pushTo(final EnergyHandler target) {
        final int offered = (int) buffer.extract(MAX_OUTPUT_PER_SIDE, Action.SIMULATE);
        if (offered == 0) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int accepted = target.insert(offered, transaction);
            if (accepted > 0) {
                transaction.commit();
                buffer.extract(accepted, Action.EXECUTE);
                setChanged();
            }
        }
    }

    private List<BlockCapabilityCache<EnergyHandler, Direction>> outputs(final ServerLevel level, final BlockPos pos) {
        if (outputs.isEmpty()) {
            final List<BlockCapabilityCache<EnergyHandler, Direction>> created = new ArrayList<>(SIDES.length);
            for (Direction side : SIDES) {
                created.add(BlockCapabilityCache.create(
                        Capabilities.Energy.BLOCK, level, pos.relative(side), side.getOpposite()));
            }
            outputs = List.copyOf(created);
        }
        return outputs;
    }

    @Override
    public Component getDisplayName() {
        return name.orDefault(getBlockState().getBlock().getName());
    }

    @Override
    public void rename(final String newName) {
        name.rename(newName);
        setChanged();
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new CoalGeneratorMenu(containerId, inventory, worldPosition);
    }

    @Override
    public void markClosed() {
        clickGuard.markClosed(level);
    }

    @Override
    public void markPlaced() {
        clickGuard.markPlaced(level);
    }

    @Override
    public boolean ignoresClick() {
        return clickGuard.ignoresClick(level);
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, fuel);
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_ENERGY, buffer.stored());
        output.putInt(TAG_BURN_LEFT, burner.burnTicksLeft());
        output.putInt(TAG_BURN_TOTAL, burner.burnTicksTotal());
        ContainerHelper.saveAllItems(output, fuel.getItems());
        name.save(output);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        buffer.restore(SimpleEnergyBuffer.Snapshot.storing(Math.max(0, input.getLongOr(TAG_ENERGY, 0))));
        burner.restore(input.getIntOr(TAG_BURN_LEFT, 0), input.getIntOr(TAG_BURN_TOTAL, 0));
        ContainerHelper.loadAllItems(input, fuel.getItems());
        name.load(input);
    }

    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        super.applyImplicitComponents(components);
        name.applyFrom(components);
    }

    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        name.collectInto(components);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(final ValueOutput output) {
        DeviceName.removeFrom(output);
    }

    /** The fuel slot; any change to it is saved with the generator. */
    private final class FuelContainer extends SimpleContainer {

        FuelContainer() {
            super(1);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            CoalGeneratorBlockEntity.this.setChanged();
        }
    }
}
