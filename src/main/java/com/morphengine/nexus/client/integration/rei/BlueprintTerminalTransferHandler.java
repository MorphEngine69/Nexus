package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.integration.BlueprintRecipeTransfer;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;

/**
 * Lays any recipe of REI out as the draft of a Blueprint Terminal: a crafting
 * recipe on the grid, any other recipe as processing, with the items and
 * fluids REI shows.
 */
final class BlueprintTerminalTransferHandler implements TransferHandler {

    @Override
    public Result handle(final Context context) {
        if (!(context.getMenu() instanceof BlueprintTerminalMenu menu)) {
            return Result.createNotApplicable();
        }
        final BlueprintRecipeTransfer transfer = transferOf(menu, context.getDisplay());
        if (transfer.isEmpty()) {
            return Result.createNotApplicable();
        }
        if (context.isActuallyCrafting()) {
            transfer.send(menu);
            return Result.createSuccessful().blocksFurtherHandling();
        }
        return Result.createSuccessful();
    }

    private static BlueprintRecipeTransfer transferOf(final BlueprintTerminalMenu menu, final Display display) {
        if (ReiGrids.isCrafting(display) && display instanceof SimpleGridMenuDisplay grid) {
            return BlueprintRecipeTransfer.crafting(ReiGrids.gridOf(grid).stream().map(ReiGrids::itemStacks).toList(),
                    menu.terminal().contents());
        }
        return BlueprintRecipeTransfer.processing(ReiGrids.amountsOf(display.getInputEntries()),
                ReiGrids.amountsOf(display.getOutputEntries()));
    }
}
