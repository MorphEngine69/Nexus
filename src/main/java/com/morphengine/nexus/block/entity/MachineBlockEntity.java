package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.config.NexusConfig;
import com.morphengine.nexus.energy.BufferUpgrades;
import com.morphengine.nexus.energy.EfficiencyUpgrades;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.machine.InputMode;
import com.morphengine.nexus.machine.Machine;
import com.morphengine.nexus.machine.MachineActivity;
import com.morphengine.nexus.machine.MachineLine;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.machine.MachineSpeed;
import com.morphengine.nexus.menu.MachineMenu;
import com.morphengine.nexus.menu.MachineView;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.processing.ItemStackSlots;
import com.morphengine.nexus.processing.MachineFacing;
import com.morphengine.nexus.processing.MachineItems;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachineOutput;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.processing.MachineRedstone;
import com.morphengine.nexus.processing.MachineTank;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transfer.ItemResource;
import com.morphengine.nexus.transport.SideMode;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A machine in the world: the machine core with its slots as stacks, the FE it draws from its network, the Speed
 * Upgrades, and what it looks like. Every tick it tops its buffer up from the energy pool of its network, which takes
 * from its sources by priority, and lets the core work; the buffer takes energy in from outside and never gives any.
 */
public final class MachineBlockEntity extends ShowcaseDeviceBlockEntity
        implements UpgradeHolder, Renamable, StandaloneDevice {

    public static final int UPGRADE_SLOTS = 4;
    public static final UpgradeLimits UPGRADE_LIMITS =
            new UpgradeLimits(Map.of(UpgradeTypes.SPEED, MachineSpeed.MAX_SPEED_UPGRADES,
                    UpgradeTypes.EFFICIENCY, EfficiencyUpgrades.MAX_UPGRADES,
                    UpgradeTypes.BUFFER, BufferUpgrades.MAX_UPGRADES));

    /** Work stops for a moment between two jobs and the model should not flicker, so it stays active that long. */
    private static final int ACTIVE_LINGER_TICKS = 10;
    private static final int RESULT_PLACE = SHOWN_PLACES - 1;
    private static final int NORMAL_SPEED_PERCENT = 100;
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_INPUTS = "inputs";
    private static final String TAG_OUTPUTS = "outputs";
    private static final String TAG_PROGRESS = "progress_";
    private static final String TAG_MODE = "input_mode";
    private static final String TAG_SIDES = "sides";
    private static final String TAG_UPGRADES = "upgrades";

    private final MachineKind kind;
    private final ItemStackSlots slots = new ItemStackSlots(this::setChanged);
    private final MachineRedstone redstone = new MachineRedstone(this::setChanged);
    private final Machine machine;
    private final MachineItems items;
    private final @Nullable MachineTank tank;
    private final UpgradeContainer upgrades =
            new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS, () -> {
                applyUpgrades();
                setChanged();
            });
    private final IEnergyStorage energyHandler;
    private MachinePhase phase = MachinePhase.OFF;
    private int lingerTicks;
    private int cooldownLeft;

    public MachineBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.MACHINE.get(), pos, state, MachineBlock::animationOf);
        final MachineBlock block = (MachineBlock) state.getBlock();
        this.kind = block.kind();
        this.tank = kind.output() == MachineOutput.FLUID
                ? new MachineTank(() -> machine().bufferCapacity(), this::setChanged) : null;
        this.machine = new Machine(block.tier(), kind.shape(), this::findRecipe,
                tank != null ? tank.slotsOver(slots) : slots);
        carryOnItem(new StoredContents(machine.energy(), tank));
        this.items = new MachineItems(slots, machine.inventory(), this::accepts, this::setChanged);
        this.energyHandler = new BufferEnergyHandler(
                machine.energy(), BufferEnergyHandler.Access.RECEIVE_ONLY, this::setChanged);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final MachineBlockEntity machine) {
        if (level instanceof ServerLevel serverLevel) {
            machine.tick(serverLevel, pos, state);
        }
    }

    public MachineKind kind() {
        return kind;
    }

    public Machine machine() {
        return machine;
    }

    public ItemStackSlots slots() {
        return slots;
    }

    public Container upgrades() {
        return upgrades;
    }

    public MachineRedstone redstone() {
        return redstone;
    }

    public IEnergyStorage energyHandler() {
        return energyHandler;
    }

    /**
     * What the machine shows a neighbour on {@code worldSide}: the slots as the setting of that side allows.
     *
     * @return the handler for the side, {@code null} when the side is closed or no side is named
     */
    public @Nullable IItemHandler itemHandler(final @Nullable Direction worldSide) {
        if (worldSide == null) {
            return null;
        }
        final Direction facing = getBlockState().getValue(MachineBlock.FACING);
        return items.handlerFor(machine.sides().mode(MachineFacing.sideOf(facing, worldSide)));
    }

    /**
     * What an Assembler sees of the machine: all of its slots and its tank, whatever the sides say, since the Assembler
     * is part of the same network and its owner has put it there.
     */
    public SideStorage assemblerAccess() {
        return new SideStorage(items.handlerFor(SideMode.BOTH), tank != null ? tank.handler() : null, null);
    }

    /**
     * Changes a side, and has other blocks ask again what the machine gives them.
     */
    public void setSideMode(final MachineSide side, final SideMode mode) {
        if (machine.sides().mode(side) != mode) {
            machine.setSideMode(side, mode);
            setChanged();
            if (level != null) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    /**
     * What the machine shows a neighbour on {@code worldSide} of its tank: the fluid to take out where the side lets
     * things out. Asked for no side, as a player filling a bucket does, it shows the tank whatever the sides say.
     *
     * @return the handler, {@code null} when the machine has no tank or the side lets nothing out
     */
    public @Nullable IFluidHandler fluidHandler(final @Nullable Direction worldSide) {
        if (tank == null) {
            return null;
        }
        if (worldSide == null) {
            return tank.handler();
        }
        final Direction facing = getBlockState().getValue(MachineBlock.FACING);
        return machine.sides().mode(MachineFacing.sideOf(facing, worldSide)).allowsOutput() ? tank.handler() : null;
    }

    public MachineView view() {
        final List<Integer> progress = new ArrayList<>();
        for (int line = 0; line < machine.lineCount(); line++) {
            progress.add(machine.line(line).progressPercent());
        }
        final SimpleEnergyBuffer energy = machine.energy();
        return new MachineView(energy.stored(), energy.capacity(), machine.speedPercent(),
                machine.activity(), progress, networkBadge(), tank != null ? tank.view() : null);
    }

    /**
     * A machine that is upgraded keeps its block entity but changes its block, and takes the size of the new tier.
     */
    @Override
    public void setBlockState(final BlockState state) {
        super.setBlockState(state);
        if (state.getBlock() instanceof MachineBlock block && block.tier().rank() > machine.tier().rank()) {
            machine.upgradeTo(block.tier());
        }
    }

    private Optional<MachineRecipe> findRecipe(final List<ResourceAmount> available) {
        return level instanceof ServerLevel serverLevel
                ? kind.recipes().find(serverLevel, available) : Optional.empty();
    }

    /**
     * @return whether the machine has a recipe for the item, so that its input slots take it
     */
    public boolean accepts(final ItemResource resource) {
        return !resource.isEmpty() && level instanceof ServerLevel serverLevel
                && kind.recipes().usesItem(serverLevel, new ItemKey(resource));
    }

    private void applyUpgrades() {
        machine.setSpeedUpgrades(upgrades.count(UpgradeTypes.SPEED));
        machine.setEfficiencyUpgrades(upgrades.count(UpgradeTypes.EFFICIENCY));
        machine.setBufferUpgrades(upgrades.count(UpgradeTypes.BUFFER));
    }

    private void tick(final ServerLevel serverLevel, final BlockPos pos, final BlockState state) {
        if (!redstone.isSignalKnown()) {
            redstone.restore(serverLevel.hasNeighborSignal(pos));
        }
        if (kind.shape().inputsPerLine() > 1) {
            for (int place = 0; place < kind.shape().inputsPerLine(); place++) {
                show(place, slots.inputs().getItem(place));
            }
            show(RESULT_PLACE, machine.line(0).activeRecipe()
                    .map(recipe -> recipe.output().resource() instanceof ItemKey key ? key.toStack(1) : ItemStack.EMPTY)
                    .orElse(ItemStack.EMPTY));
            final MachineLine line = machine.line(0);
            final Optional<MachineRecipe> active = line.activeRecipe();
            showCycle(active.map(recipe -> line.progress() / (float) (recipe.ticks() * NORMAL_SPEED_PERCENT))
                            .orElse(-1F),
                    active.map(recipe -> machine.speedPercent() / (float) (recipe.ticks() * NORMAL_SPEED_PERCENT))
                            .orElse(0F));
        }
        drawEnergy();
        machine.setEnergyUsePercent(NexusConfig.machineUsePercent());
        final boolean mayWork = redstone.permitsWork();
        final MachineActivity activity = mayWork ? machine.tick() : MachineActivity.IDLE;
        if (mayWork) {
            redstone.worked(machine);
        } else {
            machine.hold();
        }
        if (activity == MachineActivity.WORKING) {
            setChanged();
        }
        final MachinePhase next = nextPhase(activity == MachineActivity.WORKING);
        if (state.getValue(MachineBlock.PHASE) != next) {
            phase = next;
            serverLevel.setBlock(pos, state.setValue(MachineBlock.PHASE, next), Block.UPDATE_ALL);
        }
        phase = next;
    }

    /**
     * Tops the buffer up from the energy pool of the network, up to what the buffer takes in one go.
     */
    private void drawEnergy() {
        final NetworkController controller = controller();
        if (controller == null) {
            return;
        }
        final SimpleEnergyBuffer buffer = machine.energy();
        final long room = buffer.insert(machine.tier().maxInsert(), Action.SIMULATE);
        if (room > 0) {
            final long drawn = buffer.insert(controller.energy().extract(room, Action.EXECUTE), Action.EXECUTE);
            if (drawn > 0) {
                energyMeter().recordDrawn(drawn);
            }
        }
    }

    private MachinePhase nextPhase(final boolean working) {
        if (machine.energy().stored() <= 0 && !isNetworkPowered()) {
            cooldownLeft = 0;
            return MachinePhase.OFF;
        }
        if (working || lingerTicks > 0) {
            lingerTicks = working ? ACTIVE_LINGER_TICKS : lingerTicks - 1;
            return MachinePhase.ACTIVE;
        }
        if (phase == MachinePhase.ACTIVE && kind.cooldownTicks() > 0) {
            cooldownLeft = kind.cooldownTicks();
        }
        if (cooldownLeft > 0) {
            cooldownLeft--;
            return MachinePhase.COOLING;
        }
        return MachinePhase.STANDBY;
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new MachineMenu(containerId, inventory, worldPosition);
    }

    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, slots.inputs());
            Containers.dropContents(level, pos, slots.outputs());
            Containers.dropContents(level, pos, upgrades);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueOutput output = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        output.putLong(TAG_ENERGY, machine.energy().stored());
        output.saveItems(TAG_INPUTS, slots.inputStacks());
        output.saveItems(TAG_OUTPUTS, slots.outputStacks());
        for (int line = 0; line < machine.lineCount(); line++) {
            output.putLong(TAG_PROGRESS + line, machine.line(line).progress());
        }
        redstone.save(output);
        if (tank != null) {
            tank.save(output);
        }
        output.putInt(TAG_MODE, machine.inventory().mode().ordinal());
        output.putInt(TAG_SIDES, machine.sides().toBits());
        output.saveItems(TAG_UPGRADES, upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueInput input = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        machine.energy().restore(
                SimpleEnergyBuffer.Snapshot.storing(Math.max(0, input.getLongOr(TAG_ENERGY, 0))));
        input.loadItems(TAG_INPUTS, slots.inputStacks());
        input.loadItems(TAG_OUTPUTS, slots.outputStacks());
        for (int line = 0; line < machine.lineCount(); line++) {
            machine.line(line).restoreProgress(input.getLongOr(TAG_PROGRESS + line, 0));
        }
        redstone.load(input);
        if (tank != null) {
            tank.load(input);
        }
        final InputMode[] modes = InputMode.values();
        machine.inventory().setMode(modes[Math.clamp(input.getIntOr(TAG_MODE, 0), 0, modes.length - 1)]);
        restoreSides(input.getIntOr(TAG_SIDES, -1));
        input.loadItems(TAG_UPGRADES, upgrades.getItems());
        applyUpgrades();
    }

    /**
     * Brings back the saved sides; a machine saved before it had any keeps its defaults.
     */
    private void restoreSides(final int bits) {
        if (bits >= 0) {
            machine.restoreSides(bits);
        }
    }
}
