package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.integration.CraftingGridTransfer;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.terminal.GridFill;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out a crafting recipe of REI on the grid of a Crafting Terminal. A recipe
 * narrower than the grid fills it from the top left corner, as on a crafting
 * table. When some ingredients are nowhere to be found, the button turns orange
 * and lists them, but the rest can still be laid out.
 */
final class CraftingTerminalTransferHandler implements TransferHandler {

    private static final CategoryIdentifier<?> CRAFTING = CategoryIdentifier.of("minecraft", "plugins/crafting");
    private static final int GRID_SIDE = 3;
    private static final int BUTTON_MISSING = 0x80FFA500;

    @Override
    public Result handle(final Context context) {
        if (!(context.getMenu() instanceof CraftingTerminalMenu menu)
                || !(context.getDisplay() instanceof SimpleGridMenuDisplay display)
                || !CRAFTING.equals(display.getCategoryIdentifier())) {
            return Result.createNotApplicable();
        }
        final List<EntryIngredient> grid = gridOf(display);
        final CraftingGridTransfer transfer = new CraftingGridTransfer(grid.stream()
                .map(CraftingTerminalTransferHandler::itemStacks).toList());
        if (context.isActuallyCrafting()) {
            transfer.send(menu, context.isStackedCrafting() ? GridFill.MOST_CRAFTS : GridFill.ONE_CRAFT);
            return Result.createSuccessful().blocksFurtherHandling();
        }
        final List<Integer> missing = transfer.missingSlots(menu);
        if (missing.isEmpty()) {
            return Result.createSuccessful();
        }
        return Result.createSuccessful()
                .color(BUTTON_MISSING)
                .tooltip(Component.translatable("gui.nexus.recipe_transfer.missing"))
                .tooltipMissing(missing.stream().map(grid::get).toList());
    }

    /**
     * @return the recipe's ingredients placed on the nine grid slots in reading order
     */
    private static List<EntryIngredient> gridOf(final SimpleGridMenuDisplay display) {
        final List<EntryIngredient> inputs = display.getInputEntries();
        final int width = Math.max(1, display.getInputWidth(GRID_SIDE, GRID_SIDE));
        final List<EntryIngredient> grid = new ArrayList<>(GRID_SIDE * GRID_SIDE);
        for (int slot = 0; slot < GRID_SIDE * GRID_SIDE; slot++) {
            grid.add(EntryIngredient.empty());
        }
        for (int index = 0; index < inputs.size(); index++) {
            final int row = index / width;
            final int column = index % width;
            if (row < GRID_SIDE && column < GRID_SIDE) {
                grid.set(column + row * GRID_SIDE, inputs.get(index));
            }
        }
        return grid;
    }

    private static List<ItemStack> itemStacks(final EntryIngredient ingredient) {
        final List<ItemStack> stacks = new ArrayList<>(ingredient.size());
        for (EntryStack<?> entry : ingredient) {
            if (entry.getValue() instanceof ItemStack stack) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
