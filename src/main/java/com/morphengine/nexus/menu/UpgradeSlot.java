package com.morphengine.nexus.menu;

import com.morphengine.nexus.registry.NexusTags;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An upgrade slot of a device: takes one upgrade.
 */
final class UpgradeSlot extends Slot {

    UpgradeSlot(final Container container, final int index, final int x, final int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return stack.is(NexusTags.UPGRADES);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
