package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.integration.MachineRecipes;
import com.morphengine.nexus.client.screen.ExternalVaultScreen;
import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.client.screen.TransferDeviceScreen;
import com.morphengine.nexus.client.screen.VaultCellScreen;
import com.morphengine.nexus.client.screen.VoidUpgradeScreen;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.registry.NexusRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;


/**
 * JEI support: its "+" button lays out a crafting recipe on the grid of a
 * Crafting Terminal and any recipe as the draft of a Blueprint Terminal, it
 * keeps its overlay off the side buttons of terminals, Pullers, Pushers and
 * Assemblers, its recipe and usage keys work on the resources a terminal
 * lists, and its items and fluids can be dragged onto the filter of any panel
 * with one and onto the Blueprint encoder. It lists the recipes of the machines, each in a category of its own with
 * the machines of every tier as its stations, and shows the Energy Furnace as a station of the furnaces and the
 * Crafting Terminal as one of the crafting table.
 */
@JeiPlugin
public final class NexusJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registration) {
        final IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ItemProcessingCategory<>(gui, MachineRecipeTypes.CRUSHING, "gui.nexus.recipes.crushing",
                        firstOf(MachineKind.CRUSHER)),
                new ItemProcessingCategory<>(gui, MachineRecipeTypes.PULVERIZING, "gui.nexus.recipes.pulverizing",
                        firstOf(MachineKind.PULVERIZER)),
                new ItemProcessingCategory<>(gui, MachineRecipeTypes.COMPRESSING, "gui.nexus.recipes.compressing",
                        firstOf(MachineKind.COMPRESSOR)),
                new AlloyingCategory(gui, "gui.nexus.recipes.alloying", firstOf(MachineKind.ALLOY_SMELTER)),
                new ExtractingCategory(gui, "gui.nexus.recipes.extracting", firstOf(MachineKind.EXTRACTOR)));
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        registration.addRecipes(MachineRecipeTypes.CRUSHING, MachineRecipes.of(NexusRecipes.CRUSHING.get()));
        registration.addRecipes(MachineRecipeTypes.PULVERIZING, MachineRecipes.of(NexusRecipes.PULVERIZING.get()));
        registration.addRecipes(MachineRecipeTypes.COMPRESSING, MachineRecipes.of(NexusRecipes.COMPRESSING.get()));
        registration.addRecipes(MachineRecipeTypes.ALLOYING, MachineRecipes.of(NexusRecipes.ALLOYING.get()));
        registration.addRecipes(MachineRecipeTypes.EXTRACTING, MachineRecipes.of(NexusRecipes.EXTRACTING.get()));
    }

    @Override
    public void registerRecipeCatalysts(final IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(MachineRecipeTypes.CRUSHING, machines(MachineKind.CRUSHER));
        registration.addRecipeCatalysts(MachineRecipeTypes.PULVERIZING, machines(MachineKind.PULVERIZER));
        registration.addRecipeCatalysts(MachineRecipeTypes.COMPRESSING, machines(MachineKind.COMPRESSOR));
        registration.addRecipeCatalysts(MachineRecipeTypes.ALLOYING, machines(MachineKind.ALLOY_SMELTER));
        registration.addRecipeCatalysts(MachineRecipeTypes.EXTRACTING, machines(MachineKind.EXTRACTOR));
        final ItemLike[] furnaces = machines(MachineKind.ENERGY_FURNACE);
        registration.addRecipeCatalysts(RecipeTypes.SMELTING, furnaces);
        registration.addRecipeCatalysts(RecipeTypes.BLASTING, furnaces);
        registration.addRecipeCatalysts(RecipeTypes.SMOKING, furnaces);
        registration.addRecipeCatalyst(NexusItems.CRAFTING_TERMINAL.get(), RecipeTypes.CRAFTING);
    }

    private static ItemLike[] machines(final MachineKind kind) {
        return NexusItems.MACHINES.get(kind).stream().<ItemLike>map(item -> item.get()).toArray(ItemLike[]::new);
    }

    private static ItemLike firstOf(final MachineKind kind) {
        return NexusItems.MACHINES.get(kind).getFirst().get();
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
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new SideAreasHandler());
        registration.addGhostIngredientHandler(VaultCellScreen.class, new FilterGhostHandler<>());
        registration.addGhostIngredientHandler(TransferDeviceScreen.class, new FilterGhostHandler<>());
        registration.addGhostIngredientHandler(ExternalVaultScreen.class, new FilterGhostHandler<>());
        registration.addGhostIngredientHandler(VoidUpgradeScreen.class, new FilterGhostHandler<>());
        registration.addGhostIngredientHandler(terminalScreens(), new FilterGhostHandler<>());
    }

    /**
     * @return the class of every terminal screen, whatever its menu
     */
    @SuppressWarnings("unchecked")
    private static Class<TerminalScreen<?>> terminalScreens() {
        return (Class<TerminalScreen<?>>) (Class<?>) TerminalScreen.class;
    }
}
