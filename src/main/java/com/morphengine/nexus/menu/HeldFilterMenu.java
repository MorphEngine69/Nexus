package com.morphengine.nexus.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The panel of an item held in the main hand that carries a filter of ghost slots: a click on a slot lists the
 * resource without taking anything from the player. The player's inventory is shown below the filter; the slot of the
 * held item is locked while the panel is open, and the panel closes when the hand no longer holds such an item.
 */
public abstract class HeldFilterMenu extends AbstractContainerMenu implements FilterMenu {

    public static final int FILTER_SLOTS = 9;
    public static final int FILTER_LEFT = 19;
    public static final int FILTER_TOP = 62;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 104;

    private static final int SLOT_SPACING = 18;
    private static final int INVENTORY_ROWS = 3;
    private static final int ROW_LENGTH = 9;
    private static final int HOTBAR_OFFSET = 58;

    private final Player player;
    private final int heldSlot;

    protected HeldFilterMenu(final MenuType<?> type, final int containerId, final Inventory inventory) {
        super(type, containerId);
        this.player = inventory.player;
        this.heldSlot = inventory.selected;
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < ROW_LENGTH; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * ROW_LENGTH,
                        INVENTORY_LEFT + column * SLOT_SPACING, INVENTORY_TOP + row * SLOT_SPACING));
            }
        }
        for (int column = 0; column < ROW_LENGTH; column++) {
            final int x = INVENTORY_LEFT + column * SLOT_SPACING;
            final int y = INVENTORY_TOP + HOTBAR_OFFSET;
            addSlot(column == heldSlot ? new LockedSlot(inventory, column, x, y) : new Slot(inventory, column, x, y));
        }
    }

    /**
     * @return what the main hand holds, which the panel edits when {@link #holdsItem} says it is the right item
     */
    protected final ItemStack held() {
        return player.getInventory().getItem(heldSlot);
    }

    protected abstract boolean holdsItem();

    @Override
    public int filterSlotCount() {
        return FILTER_SLOTS;
    }

    @Override
    public void clicked(final int slotIndex, final int buttonNum, final ClickType input, final Player clicker) {
        if (input == ClickType.SWAP && buttonNum == heldSlot) {
            return;
        }
        super.clicked(slotIndex, buttonNum, input, clicker);
    }

    /**
     * Shift click on an item lists it in the filter; nothing moves.
     */
    @Override
    public ItemStack quickMoveStack(final Player clicker, final int slotIndex) {
        if (!clicker.level().isClientSide()) {
            addToFilter(slots.get(slotIndex).getItem());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player clicker) {
        return holdsItem();
    }
}
