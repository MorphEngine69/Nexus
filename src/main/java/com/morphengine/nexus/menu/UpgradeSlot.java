package com.morphengine.nexus.menu;

import com.morphengine.nexus.upgrade.UpgradeContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An upgrade slot of a device: takes upgrades of a kind and in a number its
 * container accepts, several copies of one kind sharing a slot where the
 * device takes more than one.
 */
final class UpgradeSlot extends Slot {

    UpgradeSlot(final Container container, final int index, final int x, final int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return container.canPlaceItem(getContainerSlot(), stack);
    }

    @Override
    public int getMaxStackSize(final ItemStack stack) {
        return container instanceof UpgradeContainer upgrades
                ? upgrades.capacityOf(getContainerSlot(), stack) : super.getMaxStackSize(stack);
    }
}
