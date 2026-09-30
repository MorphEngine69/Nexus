package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.integration.CraftingGridTransfer;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.terminal.GridFill;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Lays out a crafting recipe of REI on the grid of a Crafting Terminal. A recipe
 * narrower than the grid fills it from the top left corner, as on a crafting
 * table. When some ingredients are nowhere to be found, the button turns orange
 * and lists them, but the rest can still be laid out.
 */
final class CraftingTerminalTransferHandler implements TransferHandler {

    private static final int BUTTON_MISSING = 0x80FFA500;

    @Override
    public Result handle(final Context context) {
        if (!(context.getMenu() instanceof CraftingTerminalMenu menu)
                || !(context.getDisplay() instanceof SimpleGridMenuDisplay display)
                || !ReiGrids.isCrafting(display)) {
            return Result.createNotApplicable();
        }
        final List<EntryIngredient> grid = ReiGrids.gridOf(display);
        final CraftingGridTransfer transfer = new CraftingGridTransfer(grid.stream()
                .map(ReiGrids::itemStacks).toList());
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
}
