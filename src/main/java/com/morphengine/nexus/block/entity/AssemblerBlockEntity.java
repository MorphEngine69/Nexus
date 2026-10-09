package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.assembler.AssemblerRates;
import com.morphengine.nexus.assembler.AssemblerSettings;
import com.morphengine.nexus.automation.CraftingTask;
import com.morphengine.nexus.automation.TaskList;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.AssemblerChain;
import com.morphengine.nexus.blueprint.BlueprintCodecs;
import com.morphengine.nexus.blueprint.CraftingBlueprint;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.blueprint.ProcessingBlueprint;
import com.morphengine.nexus.energy.OperationKind;
import com.morphengine.nexus.energy.OperationUpgrades;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.AutocraftingHost;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.OperationToll;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.menu.AssemblerMenu;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.transport.TransferRate;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An Assembler. Once every few ticks, while its network has energy, it puts
 * what it crafted into the network, takes back from its machine what the
 * network's tasks wait for, and moves its own tasks on: tasks it keeps because
 * it gives what they were asked for, whichever Assembler runs their other
 * steps. Speed Upgrades make it work more often and hand out more runs at a
 * time; see {@link TransferRate}. Without energy its tasks wait, losing
 * nothing. Once a second it shows on its block whether the network has energy
 * and whether it keeps tasks.
 *
 * <p>Its machine is the one at the end of its {@link AssemblerChain}. Whether
 * the machine is waited for is up to the root of the chain, and counts the
 * inputs of the Blueprints of every Assembler in it.
 */
public final class AssemblerBlockEntity extends AnimatedDeviceBlockEntity
        implements UpgradeHolder, AutocraftingHost, Renamable {

    public static final int UPGRADE_SLOTS = 4;
    /** Placeholder balance: up to four Speed Upgrades share one slot. */
    public static final UpgradeLimits UPGRADE_LIMITS =
            new UpgradeLimits(Map.of(UpgradeTypes.SPEED, 4, UpgradeTypes.STACK, 1, UpgradeTypes.EFFICIENCY, 1,
                    UpgradeTypes.CHUNK_LOADER, 1));

    private static final int STATE_CHECK_INTERVAL_TICKS = 20;
    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_BLUEPRINTS = "blueprints";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_TASKS = "tasks";
    private static final String TAG_CRAFTED = "crafted";

    private final BlueprintSlots blueprintSlots = new BlueprintSlots(this::contentsChanged);
    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::upgradesChanged);
    private final TaskList tasks = new TaskList();
    private final AssemblerWork work = new AssemblerWork();
    private final ChainedMachine machine = new ChainedMachine();
    private AssemblerSettings settings = AssemblerSettings.DEFAULT;
    private TransferRate rate = TransferRate.BASE;
    private int cooldown;

    public AssemblerBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.ASSEMBLER.get(), pos, state, AssemblerBlock::animationOf);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final AssemblerBlockEntity assembler) {
        if (level.getGameTime() % STATE_CHECK_INTERVAL_TICKS == 0) {
            assembler.showState(level, pos, state);
        }
        if (--assembler.cooldown > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        assembler.cooldown = assembler.rate.intervalTicks();
        assembler.operate(serverLevel, pos);
    }

    private void showState(final Level level, final BlockPos pos, final BlockState state) {
        final BlockState shown = state.setValue(AssemblerBlock.POWERED, isNetworkPowered())
                .setValue(AssemblerBlock.ACTIVE, !tasks.isEmpty());
        if (shown != state) {
            level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Works for its owner: an Assembler hands inputs taken out of the network
     * to its machine and puts what was made back, so one whose owner may not
     * put into the network or take out of it stands still, holding what it has.
     */
    private void operate(final ServerLevel level, final BlockPos pos) {
        final NetworkController controller = controller();
        if (controller == null || !isNetworkPowered()) {
            return;
        }
        final Permission lacking = !ownerMay(Permission.INSERT) ? Permission.INSERT
                : ownerMay(Permission.EXTRACT) ? null : Permission.EXTRACT;
        markHalted(lacking);
        if (lacking != null) {
            return;
        }
        final Storage network = controller.resources();
        final AutocraftingComponent autocrafting = controller.component(NetworkComponentTypes.AUTOCRAFTING);
        boolean changed = work.deliver(network);
        changed |= work.collect(machine.of(level, pos), blueprintSlots.processing(), autocrafting, network);
        changed |= tasks.step(network, autocrafting.blueprints(), AssemblerRates.runsPerOperation(upgrades));
        if (changed) {
            setChanged();
        }
    }


    @Override
    public DispatchResult dispatch(final Blueprint blueprint, final List<ResourceAmount> inputs,
                                   final Action action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return DispatchResult.NO_TARGET;
        }
        final NetworkController network = controller();
        final OperationUpgrades runUpgrades = new OperationUpgrades(upgrades.count(UpgradeTypes.SPEED),
                upgrades.count(UpgradeTypes.EFFICIENCY));
        if (network != null && !OperationToll.affords(network, OperationKind.ASSEMBLER_RUN, runUpgrades)) {
            return DispatchResult.NO_ENERGY;
        }
        final EncodedBlueprint encoded = blueprintSlots.encodingOf(blueprint);
        final DispatchResult result = switch (encoded) {
            case null -> DispatchResult.NO_TARGET;
            case CraftingBlueprint crafting -> work.craft(serverLevel, crafting, inputs, action);
            case ProcessingBlueprint ignored -> dispatchProcessing(serverLevel, inputs, action);
        };
        if (result.isAccepted() && action.isExecute()) {
            if (network != null) {
                OperationToll.charge(network, OperationKind.ASSEMBLER_RUN, runUpgrades, energyMeter());
            }
            setChanged();
        }
        return result;
    }

    /**
     * Hands inputs to a machine only for an owner who may take them out of the
     * network; for anyone else the Assembler has no machine to run in.
     */
    private DispatchResult dispatchProcessing(final ServerLevel level, final List<ResourceAmount> inputs,
                                              final Action action) {
        if (!ownerMay(Permission.EXTRACT)) {
            return DispatchResult.NO_TARGET;
        }
        if (machine.isBusy(level, worldPosition, work)) {
            return DispatchResult.LOCKED;
        }
        return work.process(machine.of(level, worldPosition), inputs, action);
    }

    List<Blueprint> processingBlueprints() {
        return blueprintSlots.processing();
    }

    public Container blueprintSlots() {
        return blueprintSlots;
    }

    public Container upgrades() {
        return upgrades;
    }

    public AssemblerSettings settings() {
        return settings;
    }

    public void changeSettings(final AssemblerSettings changed) {
        final boolean priorityChanged = changed.priority() != settings.priority();
        settings = changed;
        setChanged();
        if (priorityChanged) {
            refreshNetwork();
        }
    }

    @Override
    public List<Blueprint> blueprints() {
        return blueprintSlots.blueprints();
    }

    @Override
    public int blueprintPriority() {
        return settings.priority();
    }

    @Override
    public TaskList craftingTasks() {
        return tasks;
    }

    @Override
    public void adoptTask(final CraftingTask task) {
        tasks.add(task);
        setChanged();
    }

    /**
     * Tasks show as paused while the network has no energy to move them on.
     */
    @Override
    public List<TaskStatus> taskStatuses() {
        final List<TaskStatus> statuses = tasks.statuses();
        if (isNetworkPowered()) {
            return statuses;
        }
        final List<TaskStatus> paused = new ArrayList<>(statuses.size());
        for (TaskStatus status : statuses) {
            final boolean moving = status.state() == TaskState.GATHERING || status.state() == TaskState.RUNNING;
            paused.add(moving ? status.withState(TaskState.PAUSED) : status);
        }
        return paused;
    }

    private void upgradesChanged() {
        ChunkAnchors.follow(this, upgrades);
        contentsChanged();
    }

    private void contentsChanged() {
        rate = AssemblerRates.rateOf(upgrades);
        setChanged();
        refreshNetwork();
    }

    private void refreshNetwork() {
        final NetworkController controller = controller();
        if (controller != null) {
            controller.component(NetworkComponentTypes.AUTOCRAFTING).refresh(this);
        }
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new AssemblerMenu(containerId, inventory, worldPosition);
    }

    /**
     * Broken, the Assembler drops its Blueprints and upgrades, cancels its
     * tasks and gives what they held and what it crafted back to the network;
     * items the network does not take drop too.
     */
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ChunkAnchors.release(this);
        if (level == null) {
            return;
        }
        Containers.dropContents(level, pos, blueprintSlots);
        Containers.dropContents(level, pos, upgrades);
        final List<ResourceAmount> left = new ArrayList<>(tasks.abandon());
        left.addAll(work.takeAll());
        final NetworkController controller = controller();
        Leftovers.giveBack(level, pos, controller != null ? controller.resources() : null, left);
    }

    @Override
    public void setRemoved() {
        final NetworkController controller = controller();
        if (controller != null) {
            controller.component(NetworkComponentTypes.AUTOCRAFTING).detach(this);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueOutput output = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        output.store(TAG_SETTINGS, AssemblerSettings.CODEC, settings);
        output.saveItems(TAG_BLUEPRINTS, blueprintSlots.getItems());
        output.saveItems(TAG_UPGRADES, upgrades.getItems());
        output.store(TAG_TASKS, BlueprintCodecs.TASK_CODEC.listOf(), tasks.snapshots());
        output.store(TAG_CRAFTED, NexusResources.AMOUNT_CODEC.listOf(), work.held());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueInput input = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        settings = input.read(TAG_SETTINGS, AssemblerSettings.CODEC).orElse(AssemblerSettings.DEFAULT);
        input.loadItems(TAG_BLUEPRINTS, blueprintSlots.getItems());
        input.loadItems(TAG_UPGRADES, upgrades.getItems());
        blueprintSlots.readBlueprints();
        rate = AssemblerRates.rateOf(upgrades);
        tasks.restore(input.read(TAG_TASKS, BlueprintCodecs.TASK_CODEC.listOf()).orElse(List.of()));
        work.restore(input.read(TAG_CRAFTED, NexusResources.AMOUNT_CODEC.listOf()).orElse(List.of()));
    }
}
