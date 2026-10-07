package com.morphengine.nexus.menu;

import com.morphengine.nexus.item.BlueprintItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An Assembler slot: takes one encoded Blueprint.
 */
final class BlueprintSlot extends Slot {

    BlueprintSlot(final Container container, final int index, final int x, final int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return BlueprintItem.encodedOn(stack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
