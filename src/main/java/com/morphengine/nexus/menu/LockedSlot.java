package com.morphengine.nexus.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An inventory slot shown but not usable, for the item whose panel is open:
 * moving it away would leave the panel editing nothing.
 */
final class LockedSlot extends Slot {

    LockedSlot(final Container container, final int index, final int x, final int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPickup(final Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return false;
    }
}
