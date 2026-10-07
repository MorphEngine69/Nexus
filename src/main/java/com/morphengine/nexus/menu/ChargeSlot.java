package com.morphengine.nexus.menu;

import com.morphengine.nexus.charging.ItemCharger;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The slot of an Energy Cell that charges the item in it: takes one item at a time, of any kind that stores FE.
 */
final class ChargeSlot extends Slot {

    ChargeSlot(final Container container, final int x, final int y) {
        super(container, 0, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return ItemCharger.isChargeable(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
