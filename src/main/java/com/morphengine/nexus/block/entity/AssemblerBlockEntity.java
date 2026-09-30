package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.assembler.AssemblerSettings;
import com.morphengine.nexus.assembler.LockMode;
import com.morphengine.nexus.automation.CraftingTask;
import com.morphengine.nexus.automation.TaskList;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.blueprint.BlueprintCodecs;
import com.morphengine.nexus.blueprint.CraftingBlueprint;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.blueprint.ProcessingBlueprint;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.AutocraftingHost;
import com.morphengine.nexus.level.NeighbourCapabilities;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.menu.AssemblerMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.transport.TransferRate;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public final class AssemblerBlockEntity extends NetworkDeviceBlockEntity implements AutocraftingHost, Renamable {

    public static final int UPGRADE_SLOTS = 4;
    /** Placeholder balance: up to four Speed Upgrades share one slot. */
    public static final UpgradeLimits UPGRADE_LIMITS = new UpgradeLimits(Map.of(UpgradeTypes.SPEED, 4));

    private static final Logger LOGGER = LoggerFactory.getLogger(AssemblerBlockEntity.class);
    private static final int STATE_CHECK_INTERVAL_TICKS = 20;
    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_BLUEPRINTS = "blueprints";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_TASKS = "tasks";
    private static final String TAG_CRAFTED = "crafted";

    private final BlueprintSlots blueprintSlots = new BlueprintSlots(this::contentsChanged);
    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::contentsChanged);
    private final TaskList tasks = new TaskList();
    private final AssemblerWork work = new AssemblerWork();
    private final NeighbourCapabilities neighbour = new NeighbourCapabilities();
    private AssemblerSettings settings = AssemblerSettings.DEFAULT;
    private TransferRate rate = TransferRate.BASE;
    private int cooldown;

    public AssemblerBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.ASSEMBLER.get(), pos, state);
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
        assembler.operate(serverLevel, pos, state.getValue(AssemblerBlock.FACING));
    }

    private void showState(final Level level, final BlockPos pos, final BlockState state) {
        final BlockState shown = state.setValue(AssemblerBlock.POWERED, isNetworkPowered())
                .setValue(AssemblerBlock.ACTIVE, !tasks.isEmpty());
        if (shown != state) {
            level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
        }
    }

    private void operate(final ServerLevel level, final BlockPos pos, final Direction face) {
        final NetworkController controller = controller();
        if (controller == null || !isNetworkPowered()) {
            return;
        }
        final Storage network = controller.resources();
        final AutocraftingComponent autocrafting = controller.component(NetworkComponentTypes.AUTOCRAFTING);
        boolean changed = work.deliver(network);
        changed |= work.collect(neighbour.itemsAndFluids(level, pos, face), blueprintSlots.processing(),
                autocrafting, network);
        changed |= tasks.step(network, autocrafting.blueprints(), dispatchesPerOperation());
        if (changed) {
            setChanged();
        }
    }

    /**
     * @return runs the Assembler's tasks hand out per operation: one, and one
     *         more for every Speed Upgrade
     */
    private int dispatchesPerOperation() {
        return 1 + upgrades.count(UpgradeTypes.SPEED);
    }

    @Override
    public DispatchResult dispatch(final Blueprint blueprint, final List<ResourceAmount> inputs,
                                   final Action action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return DispatchResult.NO_TARGET;
        }
        final EncodedBlueprint encoded = blueprintSlots.encodingOf(blueprint);
        final DispatchResult result = switch (encoded) {
            case null -> DispatchResult.NO_TARGET;
            case CraftingBlueprint crafting -> work.craft(serverLevel, crafting, inputs, action);
            case ProcessingBlueprint _ -> dispatchProcessing(serverLevel, inputs, action);
        };
        if (result.isAccepted() && action.isExecute()) {
            setChanged();
        }
        return result;
    }

    private DispatchResult dispatchProcessing(final ServerLevel level, final List<ResourceAmount> inputs,
                                              final Action action) {
        final SideStorage machine = neighbour.itemsAndFluids(level, worldPosition,
                getBlockState().getValue(AssemblerBlock.FACING));
        if (settings.lock() == LockMode.UNTIL_EMPTY && work.isBusy(machine, blueprintSlots.processing())) {
            return DispatchResult.LOCKED;
        }
        return work.process(machine, inputs, action);
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

    private void contentsChanged() {
        rate = TransferRate.of(upgrades.count(UpgradeTypes.SPEED), 0);
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
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) {
            return;
        }
        Containers.dropContents(level, pos, blueprintSlots);
        Containers.dropContents(level, pos, upgrades);
        final List<ResourceAmount> left = new ArrayList<>(tasks.abandon());
        left.addAll(work.takeAll());
        giveBack(level, pos, left);
    }

    private void giveBack(final Level world, final BlockPos pos, final List<ResourceAmount> left) {
        final NetworkController controller = controller();
        final Storage network = controller != null ? controller.resources() : null;
        for (ResourceAmount amount : left) {
            final long inserted = network != null
                    ? network.insert(amount.resource(), amount.amount(), Action.EXECUTE, Actor.NOBODY) : 0;
            final long rest = amount.amount() - inserted;
            if (rest > 0 && amount.resource() instanceof ItemKey item) {
                dropItems(world, pos, item, rest);
            } else if (rest > 0) {
                LOGGER.warn("Assembler at {} was broken holding {} of {} the network did not take",
                        pos, rest, NexusResources.of(amount.resource()).id());
            }
        }
    }

    private static void dropItems(final Level world, final BlockPos pos, final ItemKey item, final long amount) {
        long left = amount;
        while (left > 0) {
            final int count = (int) Math.min(left, item.maxStackSize());
            Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), item.toStack(count));
            left -= count;
        }
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
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_SETTINGS, AssemblerSettings.CODEC, settings);
        ContainerHelper.saveAllItems(output.child(TAG_BLUEPRINTS), blueprintSlots.getItems());
        ContainerHelper.saveAllItems(output.child(TAG_UPGRADES), upgrades.getItems());
        output.store(TAG_TASKS, BlueprintCodecs.TASK_CODEC.listOf(), tasks.snapshots());
        output.store(TAG_CRAFTED, NexusResources.AMOUNT_CODEC.listOf(), work.held());
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        settings = input.read(TAG_SETTINGS, AssemblerSettings.CODEC).orElse(AssemblerSettings.DEFAULT);
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_BLUEPRINTS), blueprintSlots.getItems());
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_UPGRADES), upgrades.getItems());
        blueprintSlots.readBlueprints();
        rate = TransferRate.of(upgrades.count(UpgradeTypes.SPEED), 0);
        tasks.restore(input.read(TAG_TASKS, BlueprintCodecs.TASK_CODEC.listOf()).orElse(List.of()));
        work.restore(input.read(TAG_CRAFTED, NexusResources.AMOUNT_CODEC.listOf()).orElse(List.of()));
    }
}
