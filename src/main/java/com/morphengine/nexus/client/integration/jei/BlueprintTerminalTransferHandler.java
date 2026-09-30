package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.blueprint.GridSlot;
import com.morphengine.nexus.client.integration.BlueprintRecipeTransfer;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.registry.NexusMenuTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lays any recipe of JEI out as the draft of a Blueprint Terminal: a crafting
 * recipe on the grid, whose nine input slots are the grid in reading order,
 * and any other recipe as processing, with the items and fluids JEI shows.
 */
final class BlueprintTerminalTransferHandler implements IUniversalRecipeTransferHandler<BlueprintTerminalMenu> {

    @Override
    public Class<? extends BlueprintTerminalMenu> getContainerClass() {
        return BlueprintTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<BlueprintTerminalMenu>> getMenuType() {
        return Optional.of(NexusMenuTypes.BLUEPRINT_TERMINAL.get());
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            final BlueprintTerminalMenu menu, final Object recipe, final IRecipeSlotsView recipeSlots,
            final Player player, final boolean maxTransfer, final boolean doTransfer) {
        final BlueprintRecipeTransfer transfer = transferOf(menu, recipe, recipeSlots);
        if (doTransfer && !transfer.isEmpty()) {
            transfer.send(menu);
        }
        return null;
    }

    private static BlueprintRecipeTransfer transferOf(
            final BlueprintTerminalMenu menu, final Object recipe, final IRecipeSlotsView recipeSlots) {
        final List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        final boolean crafting = recipe instanceof RecipeHolder<?> holder && holder.value() instanceof CraftingRecipe
                && inputs.size() == GridSlot.COUNT;
        if (crafting) {
            return BlueprintRecipeTransfer.crafting(inputs.stream().map(slot -> slot.getItemStacks().toList()).toList(),
                    menu.terminal().contents());
        }
        return BlueprintRecipeTransfer.processing(amountsOf(inputs),
                amountsOf(recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)));
    }

    /**
     * @return what the slots show, items with their count and fluids in
     *         millibuckets; empty slots are left out
     */
    private static List<ResourceAmount> amountsOf(final List<IRecipeSlotView> slots) {
        final List<ResourceAmount> amounts = new ArrayList<>(slots.size());
        for (IRecipeSlotView slot : slots) {
            final ResourceAmount item = slot.getDisplayedItemStack().map(BlueprintRecipeTransfer::amountOf)
                    .orElse(null);
            final ResourceAmount fluid = slot.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK)
                    .map(BlueprintRecipeTransfer::amountOf).orElse(null);
            if (item != null) {
                amounts.add(item);
            } else if (fluid != null) {
                amounts.add(fluid);
            }
        }
        return amounts;
    }
}
