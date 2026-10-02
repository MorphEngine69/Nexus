package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.client.integration.CraftingGridTransfer;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.terminal.GridFill;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Lays out a crafting recipe of JEI on the grid of a Crafting Terminal, or of a
 * Nexus Terminal working as one. The nine input slots of JEI's crafting
 * category are the grid in reading order. Slots whose ingredient neither the
 * player nor the network holds are marked, but the rest of the recipe can
 * still be laid out.
 */
final class CraftingTerminalTransferHandler
        implements IRecipeTransferHandler<CraftingTerminalMenu, RecipeHolder<CraftingRecipe>> {

    private final Supplier<MenuType<CraftingTerminalMenu>> menuType;

    /**
     * @param menuType the menu type of the terminal block or of the Nexus Terminal
     */
    CraftingTerminalTransferHandler(final Supplier<MenuType<CraftingTerminalMenu>> menuType) {
        this.menuType = menuType;
    }

    @Override
    public Class<? extends CraftingTerminalMenu> getContainerClass() {
        return CraftingTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingTerminalMenu>> getMenuType() {
        return Optional.of(menuType.get());
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    /**
     * Still abstract in JEI, which calls it from the default of the method replacing it.
     */
    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(
            final CraftingTerminalMenu menu, final RecipeHolder<CraftingRecipe> recipe,
            final IRecipeSlotsView recipeSlots, final Player player, final boolean maxTransfer,
            final boolean doTransfer) {
        final List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        final CraftingGridTransfer transfer = new CraftingGridTransfer(
                inputs.stream().map(slot -> slot.getItemStacks().toList()).toList());
        if (doTransfer) {
            transfer.send(menu, maxTransfer ? GridFill.MOST_CRAFTS : GridFill.ONE_CRAFT);
            return null;
        }
        final List<Integer> missing = transfer.missingSlots(menu);
        return missing.isEmpty() ? null : new MissingIngredientsError(missing.stream().map(inputs::get).toList());
    }
}
