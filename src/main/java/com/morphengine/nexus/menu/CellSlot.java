package com.morphengine.nexus.menu;

import com.morphengine.nexus.item.VaultCellItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A Storage Vault slot: takes one Vault Cell.
 */
final class CellSlot extends Slot {

    CellSlot(final Container container, final int index, final int x, final int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return stack.getItem() instanceof VaultCellItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
