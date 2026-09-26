package com.morphengine.nexus.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The result slot of a Crafting Terminal. Once a craft has used up the grid,
 * the grid is filled again from what it held, so the next craft is ready.
 */
final class TerminalResultSlot extends ResultSlot {

    private final CraftingContainer grid;
    private final Consumer<List<ItemStack>> refill;

    /**
     * @param refill told the grid's items before the craft, one copy per grid slot
     */
    TerminalResultSlot(
            final Player player, final CraftingContainer grid, final Container result, final int x, final int y,
            final Consumer<List<ItemStack>> refill) {
        super(player, grid, result, 0, x, y);
        this.grid = grid;
        this.refill = refill;
    }

    @Override
    public void onTake(final Player player, final ItemStack carried) {
        final List<ItemStack> before = new ArrayList<>(grid.getContainerSize());
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            before.add(grid.getItem(slot).copy());
        }
        super.onTake(player, carried);
        refill.accept(before);
    }
}
