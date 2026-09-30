package com.morphengine.nexus.menu;

import com.morphengine.nexus.assembler.AssemblerSettings;
import com.morphengine.nexus.assembler.LockMode;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.BlueprintSlots;
import com.morphengine.nexus.item.BlueprintItem;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.EnumCycle;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Assembler panel: its priority with buttons to change it, the Blueprint
 * slots, the upgrade slots, the player's inventory, and a button for whether
 * it waits for its machine. Buttons go through the vanilla menu button packet;
 * the settings and the number of tasks return in data slots.
 */
public final class AssemblerMenu extends DeviceMenu<AssemblerBlockEntity> implements NetworkBadgeView {

    public static final int BLUEPRINTS_LEFT = 19;
    public static final int BLUEPRINTS_TOP = 64;
    public static final int BLUEPRINT_COLUMNS = 3;
    public static final int UPGRADES_LEFT = 196;
    public static final int UPGRADES_TOP = BLUEPRINTS_TOP;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 142;

    /** Menu button ids: the priority buttons, then the lock, forwards and backwards. */
    public static final int BUTTON_PRIORITY = 0;
    public static final int BUTTON_LOCK = BUTTON_PRIORITY + PriorityButtons.count();

    private static final int SLOT_SPACING = 18;
    private static final int OWN_SLOTS = BlueprintSlots.SIZE + AssemblerBlockEntity.UPGRADE_SLOTS;

    private final DataSlot priority = DataSlot.standalone();
    private final DataSlot lock = DataSlot.standalone();
    private final DataSlot taskCount = DataSlot.standalone();
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private @Nullable NetworkBadge badge;

    public AssemblerMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.ASSEMBLER.get(), containerId, inventory, pos, AssemblerBlockEntity.class);
        final AssemblerBlockEntity assembler = blockEntity();
        final boolean onServer = viewer() != null && assembler != null;
        final Container blueprints = onServer ? assembler.blueprintSlots() : new SimpleContainer(BlueprintSlots.SIZE);
        final Container upgrades = onServer ? assembler.upgrades() : new UpgradeContainer(
                AssemblerBlockEntity.UPGRADE_SLOTS, AssemblerBlockEntity.UPGRADE_LIMITS, () -> { });
        for (int slot = 0; slot < BlueprintSlots.SIZE; slot++) {
            addSlot(new BlueprintSlot(blueprints, slot, BLUEPRINTS_LEFT + slot % BLUEPRINT_COLUMNS * SLOT_SPACING,
                    BLUEPRINTS_TOP + slot / BLUEPRINT_COLUMNS * SLOT_SPACING));
        }
        for (int slot = 0; slot < AssemblerBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADES_LEFT, UPGRADES_TOP + slot * SLOT_SPACING));
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(priority);
        addDataSlot(lock);
        addDataSlot(taskCount);
    }

    public int priority() {
        return priority.get();
    }

    public LockMode lock() {
        return LockMode.values()[Math.floorMod(lock.get(), LockMode.values().length)];
    }

    public int taskCount() {
        return taskCount.get();
    }

    @Override
    public @Nullable NetworkBadge badge() {
        return badge;
    }

    @Override
    public void acceptBadge(final @Nullable NetworkBadge received) {
        badge = received;
    }

    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        final AssemblerBlockEntity assembler = blockEntity();
        if (assembler == null) {
            return false;
        }
        final AssemblerSettings current = assembler.settings();
        final int priorityIndex = buttonId - BUTTON_PRIORITY;
        if (priorityIndex >= 0 && priorityIndex < PriorityButtons.count()) {
            assembler.changeSettings(current.withPriority(current.priority() + PriorityButtons.stepOf(priorityIndex)));
            return true;
        }
        if (buttonId == BUTTON_LOCK || buttonId == BUTTON_LOCK + 1) {
            assembler.changeSettings(current.withLock(EnumCycle.step(current.lock(), buttonId != BUTTON_LOCK)));
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        final AssemblerBlockEntity assembler = blockEntity();
        final ServerPlayer viewer = viewer();
        if (assembler != null && viewer != null) {
            priority.set(assembler.settings().priority());
            lock.set(assembler.settings().lock().ordinal());
            taskCount.set(assembler.taskStatuses().size());
            badgeSync.tick(viewer, containerId, assembler.networkBadge());
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final boolean moved = slotIndex < OWN_SLOTS
                ? moveItemStackTo(stack, OWN_SLOTS, slots.size(), true)
                : moveIntoDevice(stack);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private boolean moveIntoDevice(final ItemStack stack) {
        if (BlueprintItem.encodedOn(stack) != null) {
            return moveItemStackTo(stack, 0, BlueprintSlots.SIZE, false);
        }
        return AssemblerBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)
                && moveItemStackTo(stack, BlueprintSlots.SIZE, OWN_SLOTS, false);
    }
}
