package com.morphengine.nexus.client.integration.emi;

import com.morphengine.nexus.client.integration.CraftingGridTransfer;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.terminal.GridFill;
import com.morphengine.nexus.terminal.TerminalEntry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out a crafting recipe of EMI on the grid of a Crafting Terminal, taking what it needs from the inventory first
 * and then from the network. What EMI counts as at hand is the inventory and the grid together with everything the
 * network holds.
 */
final class CraftingTerminalHandler implements EmiRecipeHandler<CraftingTerminalMenu> {

    private static final int GRID_SLOTS = 9;

    @Override
    public EmiPlayerInventory getInventory(final AbstractContainerScreen<CraftingTerminalMenu> screen) {
        final CraftingTerminalMenu menu = screen.getMenu();
        final List<EmiStack> stacks = new ArrayList<>();
        for (ItemStack stack : menu.ingredientsAtHand()) {
            if (!stack.isEmpty()) {
                stacks.add(EmiStack.of(stack));
            }
        }
        for (TerminalEntry entry : menu.terminal().contents().entries()) {
            if (entry.resource() instanceof ItemKey item) {
                stacks.add(EmiStack.of(item.toStack(1), entry.amount()));
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(final EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
    }

    @Override
    public boolean alwaysDisplaySupport(final EmiRecipe recipe) {
        return supportsRecipe(recipe);
    }

    @Override
    public boolean canCraft(final EmiRecipe recipe, final EmiCraftContext<CraftingTerminalMenu> context) {
        final CraftingGridTransfer transfer = transferOf(recipe);
        final int wanted = (int) recipe.getInputs().stream().filter(input -> !input.isEmpty()).count();
        return transfer.missingSlots(context.getScreenHandler()).size() < wanted;
    }

    @Override
    public boolean craft(final EmiRecipe recipe, final EmiCraftContext<CraftingTerminalMenu> context) {
        transferOf(recipe).send(context.getScreenHandler(),
                context.getAmount() > 1 ? GridFill.MOST_CRAFTS : GridFill.ONE_CRAFT);
        Minecraft.getInstance().setScreen(context.getScreen());
        return true;
    }

    /**
     * @return the recipe on the nine grid slots, its ingredients in the order EMI lists them
     */
    private static CraftingGridTransfer transferOf(final EmiRecipe recipe) {
        return new CraftingGridTransfer(gridOf(recipe));
    }

    static List<List<ItemStack>> gridOf(final EmiRecipe recipe) {
        final List<EmiIngredient> inputs = recipe.getInputs();
        final List<List<ItemStack>> grid = new ArrayList<>(GRID_SLOTS);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            grid.add(slot < inputs.size() ? itemsOf(inputs.get(slot)) : List.of());
        }
        return grid;
    }

    private static List<ItemStack> itemsOf(final EmiIngredient ingredient) {
        return ingredient.getEmiStacks().stream().map(EmiStack::getItemStack).filter(stack -> !stack.isEmpty())
                .toList();
    }
}
