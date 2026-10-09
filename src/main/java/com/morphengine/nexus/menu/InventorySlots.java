package com.morphengine.nexus.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.function.Consumer;

/**
 * Moves the player's inventory slots of a menu, added in the vanilla order:
 * three rows of the main inventory, then the hotbar below them.
 */
final class InventorySlots {

    static final int COUNT = 36;

    private static final int COLUMNS = 9;
    private static final int MAIN_SLOTS = 27;
    private static final int SPACING = 18;
    private static final int HOTBAR_OFFSET = 58;

    private InventorySlots() {
    }

    /**
     * Adds the slots of the inventory of the player to a menu, in the vanilla order.
     *
     * @param addSlot adds a slot to the menu
     */
    static void add(final Consumer<Slot> addSlot, final Inventory inventory, final int left, final int top) {
        for (int index = 0; index < COUNT; index++) {
            final int row = index < MAIN_SLOTS ? index / COLUMNS : 0;
            final int slotIndex = index < MAIN_SLOTS ? index + COLUMNS : index - MAIN_SLOTS;
            final int x = left + (index < MAIN_SLOTS ? index % COLUMNS : index - MAIN_SLOTS) * SPACING;
            final int y = top + (index < MAIN_SLOTS ? row * SPACING : HOTBAR_OFFSET);
            addSlot.accept(new Slot(inventory, slotIndex, x, y));
        }
    }

    /**
     * @param first index in {@code slots} of the first main inventory slot
     */
    static void place(final List<Slot> slots, final int first, final int left, final int top) {
        for (int index = 0; index < COUNT; index++) {
            final Slot slot = slots.get(first + index);
            if (index < MAIN_SLOTS) {
                slot.x = left + index % COLUMNS * SPACING;
                slot.y = top + index / COLUMNS * SPACING;
            } else {
                slot.x = left + (index - MAIN_SLOTS) * SPACING;
                slot.y = top + HOTBAR_OFFSET;
            }
        }
    }
}
