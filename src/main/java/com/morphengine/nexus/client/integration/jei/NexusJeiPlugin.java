package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.client.screen.VaultCellScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;

/**
 * JEI support: its "+" button lays out a crafting recipe on the grid of a
 * Crafting Terminal, it keeps its overlay off the terminal's side buttons, its
 * recipe and usage keys work on the resources a terminal lists, and its items
 * and fluids can be dragged onto the filter of a Vault Cell.
 */
@JeiPlugin
public final class NexusJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(final IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CraftingTerminalTransferHandler(), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(TerminalScreen.class, new TerminalGuiHandler());
        registration.addGhostIngredientHandler(VaultCellScreen.class, new VaultCellGhostHandler());
    }
}
