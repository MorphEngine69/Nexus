package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.energy.BufferUpgrades;
import com.morphengine.nexus.energy.EfficiencyUpgrades;
import com.morphengine.nexus.energy.FuelBurner;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.generator.BucketSlot;
import com.morphengine.nexus.generator.GeneratorFuel.FluidFuel;
import com.morphengine.nexus.generator.GeneratorFuel.ItemFuel;
import com.morphengine.nexus.generator.GeneratorItems;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.generator.GeneratorSides;
import com.morphengine.nexus.generator.GeneratorTanks;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.menu.GeneratorMenu;
import com.morphengine.nexus.menu.GeneratorView;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Burns fuel into its own buffer and passes the energy on: first into the energy pool of its network, then into
 * neighbouring blocks that accept FE. It never draws energy from the network. Each Speed Upgrade burns the fuel one
 * time faster, so the same fuel gives its energy sooner. What it burns, and what its input slot takes, is the
 * {@link GeneratorKind} of its block: items in a fuel slot, or fluids in tanks filled by bucket, by the slot of the
 * panel or by pipe.
 */
public final class GeneratorBlockEntity extends AnimatedDeviceBlockEntity implements Renamable {

    /** Placeholder balance, like the other numbers of the generators. */
    public static final int MAX_SPEED_UPGRADES = 4;
    public static final int UPGRADE_SLOTS = 4;
    public static final UpgradeLimits UPGRADE_LIMITS =
            new UpgradeLimits(Map.of(UpgradeTypes.SPEED, MAX_SPEED_UPGRADES, UpgradeTypes.CHUNK_LOADER, 1,
                    UpgradeTypes.EFFICIENCY, EfficiencyUpgrades.MAX_UPGRADES,
                    UpgradeTypes.BUFFER, BufferUpgrades.MAX_UPGRADES));

    /** Work stops for a moment between two pieces of fuel and the model should not flicker, so it stays at work. */
    private static final int ACTIVE_LINGER_TICKS = 10;
    /** Placeholder balance until the numbers are settled. */
    private static final long CAPACITY = 100_000;
    private static final long MAX_OUTPUT_PER_SIDE = 1_000;

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_BURN_LEFT = "burn_left";
    private static final String TAG_BURN_TOTAL = "burn_total";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_TANKS = "tanks";
    private static final String TAG_SIDES = "sides";

    private final GeneratorKind kind;
    private final SimpleEnergyBuffer buffer;
    private final FuelBurner burner;
    private final SimpleContainer input = new InputContainer();
    private final @Nullable GeneratorTanks tanks;
    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::upgradesChanged);
    private final EnergyHandler handler;
    private final GeneratorItems items;
    private final NeighbourEnergyOutputs outputs = new NeighbourEnergyOutputs(MAX_OUTPUT_PER_SIDE);
    private final GeneratorSides sides = new GeneratorSides();
    private long producedLastTick;
    private int lingerTicks;

    public GeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.GENERATOR.get(), pos, state, GeneratorBlock::animationOf);
        this.kind = ((GeneratorBlock) state.getBlock()).kind();
        this.buffer = new SimpleEnergyBuffer(CAPACITY, maxInsertPerTick(kind), MAX_OUTPUT_PER_SIDE);
        this.burner = new FuelBurner(kind.energyPerTick());
        this.handler = new BufferEnergyHandler(buffer, BufferEnergyHandler.Access.GIVE_ONLY, this::setChanged);
        this.tanks = kind.fuel().tanks().isEmpty() ? null
                : new GeneratorTanks(kind.fuel().tanks(), GeneratorTanks.CAPACITY_MILLIBUCKETS,
                        this::setChanged);
        carryOnItem(new StoredContents(buffer, tanks));
        this.items = new GeneratorItems(input.getItems(), kind.itemRules(), this::setChanged);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final GeneratorBlockEntity generator) {
        if (level instanceof ServerLevel serverLevel) {
            generator.tick(serverLevel, pos, state);
        }
    }

    public GeneratorKind kind() {
        return kind;
    }

    /**
     * @return the input slot: the fuel slot, or the slot of the bucket
     */
    public Container input() {
        return input;
    }

    public Container upgrades() {
        return upgrades;
    }

    /**
     * What a neighbour on {@code worldSide} sees of the energy: what the generator gives, where the side lets it out.
     *
     * @return the handler, {@code null} when the side gives nothing
     */
    public @Nullable EnergyHandler energyHandler(final @Nullable Direction worldSide) {
        return worldSide == null || sides.modeOn(getBlockState(), worldSide).allowsOutput() ? handler : null;
    }

    /**
     * @return the tanks as a neighbour on {@code worldSide} sees them, {@code null} when the generator burns items or
     *         the side lets nothing in
     */
    public @Nullable ResourceHandler<FluidResource> fluidHandler(final @Nullable Direction worldSide) {
        return worldSide == null || sides.modeOn(getBlockState(), worldSide).allowsInput() ? tanks : null;
    }

    /**
     * @return the input slot as a neighbour on {@code worldSide} sees it, {@code null} when the side is closed or no
     *         side is named
     */
    public @Nullable ResourceHandler<ItemResource> itemHandler(final @Nullable Direction worldSide) {
        return worldSide == null ? null : items.handlerFor(sides.modeOn(getBlockState(), worldSide));
    }

    public SideConfig<MachineSide> sides() {
        return sides.config();
    }

    /**
     * Changes a side, and has other blocks ask again what the generator gives them.
     */
    public void setSideMode(final MachineSide side, final SideMode mode) {
        if (sides.set(side, mode)) {
            setChanged();
            if (level != null) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    private static long maxInsertPerTick(final GeneratorKind kind) {
        return EfficiencyUpgrades.yielded(kind.energyPerTick() * (1 + MAX_SPEED_UPGRADES),
                EfficiencyUpgrades.MAX_UPGRADES);
    }

    private void applyUpgrades() {
        burner.setSpeed(1 + upgrades.count(UpgradeTypes.SPEED));
        burner.setEfficiencyUpgrades(upgrades.count(UpgradeTypes.EFFICIENCY));
        final int bufferUpgrades = upgrades.count(UpgradeTypes.BUFFER);
        buffer.resize(BufferUpgrades.scaled(CAPACITY, bufferUpgrades), maxInsertPerTick(kind), MAX_OUTPUT_PER_SIDE);
        if (tanks != null) {
            tanks.setCapacityMillibuckets(
                    (int) BufferUpgrades.scaled(GeneratorTanks.CAPACITY_MILLIBUCKETS, bufferUpgrades));
        }
    }

    private void upgradesChanged() {
        applyUpgrades();
        ChunkAnchors.follow(this, upgrades);
        setChanged();
    }

    public GeneratorView view() {
        return new GeneratorView(buffer.stored(), buffer.capacity(), producedLastTick, burner.burnTicksLeft(),
                burner.burnTicksTotal(), networkBadge(), tanks == null ? List.of() : tanks.views(),
                kind.acceptedFluids());
    }

    private void tick(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (tanks != null && BucketSlot.drain(input, tanks)) {
            setChanged();
        }
        if (!burner.isBurning() && burner.hasRoomIn(buffer)) {
            igniteNextFuel(level);
        }
        producedLastTick = burner.tick(buffer);
        feedNetwork();
        if (outputs.push(level, pos, buffer, sides.outputMask(state))) {
            setChanged();
        }
        updatePhase(level, pos, state);
        if (producedLastTick > 0) {
            setChanged();
        }
    }

    private void updatePhase(final ServerLevel level, final BlockPos pos, final BlockState state) {
        final MachinePhase next = nextPhase();
        if (state.getValue(GeneratorBlock.PHASE) != next) {
            level.setBlock(pos, state.setValue(GeneratorBlock.PHASE, next), Block.UPDATE_ALL);
        }
    }

    private MachinePhase nextPhase() {
        if (producedLastTick > 0 || lingerTicks > 0) {
            lingerTicks = producedLastTick > 0 ? ACTIVE_LINGER_TICKS : lingerTicks - 1;
            return MachinePhase.ACTIVE;
        }
        return buffer.stored() <= 0 && !isNetworkPowered() ? MachinePhase.OFF : MachinePhase.STANDBY;
    }

    private void igniteNextFuel(final ServerLevel level) {
        switch (kind.fuel()) {
            case ItemFuel item -> igniteItem(level, item);
            case FluidFuel fluid -> igniteFluid(fluid);
        }
    }

    private void igniteItem(final ServerLevel level, final ItemFuel fuel) {
        final ItemStack stack = input.getItem(0);
        if (!fuel.accepts().test(stack)) {
            return;
        }
        final int burnTicks = fuel.burnTicks().applyAsInt(level, stack);
        if (burnTicks > 0) {
            stack.shrink(1);
            burner.ignite(burnTicks);
            setChanged();
        }
    }

    private void igniteFluid(final FluidFuel fuel) {
        if (tanks != null && tanks.holdsPortion()) {
            tanks.takePortion();
            burner.ignite(fuel.burnTicksPerPortion());
            setChanged();
        }
    }

    private void feedNetwork() {
        final NetworkController controller = controller();
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

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new GeneratorMenu(containerId, inventory, worldPosition);
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ChunkAnchors.release(this);
        if (level != null) {
            Containers.dropContents(level, pos, input);
            Containers.dropContents(level, pos, upgrades);
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_ENERGY, buffer.stored());
        output.putInt(TAG_BURN_LEFT, burner.burnTicksLeft());
        output.putInt(TAG_BURN_TOTAL, burner.burnTicksTotal());
        ContainerHelper.saveAllItems(output, input.getItems());
        ContainerHelper.saveAllItems(output.child(TAG_UPGRADES), upgrades.getItems());
        if (tanks != null) {
            tanks.serialize(output.child(TAG_TANKS));
        }
        output.putInt(TAG_SIDES, sides.config().toBits());
    }

    @Override
    protected void loadAdditional(final ValueInput source) {
        super.loadAdditional(source);
        buffer.restore(SimpleEnergyBuffer.Snapshot.storing(Math.max(0, source.getLongOr(TAG_ENERGY, 0))));
        burner.restore(source.getIntOr(TAG_BURN_LEFT, 0), source.getIntOr(TAG_BURN_TOTAL, 0));
        ContainerHelper.loadAllItems(source, input.getItems());
        ContainerHelper.loadAllItems(source.childOrEmpty(TAG_UPGRADES), upgrades.getItems());
        if (tanks != null) {
            tanks.deserialize(source.childOrEmpty(TAG_TANKS));
        }
        sides.restore(source.getIntOr(TAG_SIDES, -1));
        applyUpgrades();
    }

    /** The input slot; any change to it is saved with the generator. */
    private final class InputContainer extends SimpleContainer {

        InputContainer() {
            super(1);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            GeneratorBlockEntity.this.setChanged();
        }
    }
}
