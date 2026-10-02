package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.screen.AssemblerScreen;
import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.client.screen.TransferDeviceScreen;
import com.morphengine.nexus.client.screen.VaultCellScreen;
import com.morphengine.nexus.registry.NexusMenuTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * JEI support: its "+" button lays out a crafting recipe on the grid of a
 * Crafting Terminal and any recipe as the draft of a Blueprint Terminal, it
 * keeps its overlay off the side buttons of terminals, Pullers, Pushers and
 * Assemblers, its recipe and usage keys work on the resources a terminal
 * lists, and its items and fluids can be dragged onto the filter of any panel
 * with one and onto the Blueprint encoder.
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
        registration.addRecipeTransferHandler(new CraftingTerminalTransferHandler(NexusMenuTypes.CRAFTING_TERMINAL),
                RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(
                new CraftingTerminalTransferHandler(NexusMenuTypes.PORTABLE_CRAFTING_TERMINAL), RecipeTypes.CRAFTING);
        registration.addUniversalRecipeTransferHandler(
                new BlueprintTerminalTransferHandler(NexusMenuTypes.BLUEPRINT_TERMINAL));
        registration.addUniversalRecipeTransferHandler(
                new BlueprintTerminalTransferHandler(NexusMenuTypes.PORTABLE_BLUEPRINT_TERMINAL));
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(TerminalScreen.class, new TerminalGuiHandler());
        registration.addGuiContainerHandler(TransferDeviceScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(final TransferDeviceScreen screen) {
                return List.of(screen.sidebarArea());
            }
        });
        registration.addGhostIngredientHandler(VaultCellScreen.class, new FilterGhostHandler<>());
        registration.addGhostIngredientHandler(TransferDeviceScreen.class, new FilterGhostHandler<>());
        registration.addGhostIngredientHandler(terminalScreens(), new FilterGhostHandler<>());
        registration.addGuiContainerHandler(AssemblerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(final AssemblerScreen screen) {
                return List.of(screen.sidebarArea());
            }
        });
    }

    /**
     * @return the class of every terminal screen, whatever its menu
     */
    @SuppressWarnings("unchecked")
    private static Class<TerminalScreen<?>> terminalScreens() {
        return (Class<TerminalScreen<?>>) (Class<?>) TerminalScreen.class;
    }
}
