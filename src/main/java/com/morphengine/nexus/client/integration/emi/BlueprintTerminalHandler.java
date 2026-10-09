package com.morphengine.nexus.client.integration.emi;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.client.integration.BlueprintRecipeTransfer;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays any recipe of EMI out as the draft of a Blueprint Terminal: a crafting recipe on the grid, any other recipe as
 * processing, with the items and fluids EMI shows.
 */
final class BlueprintTerminalHandler implements EmiRecipeHandler<BlueprintTerminalMenu> {

    @Override
    public EmiPlayerInventory getInventory(final AbstractContainerScreen<BlueprintTerminalMenu> screen) {
        return new EmiPlayerInventory(List.of());
    }

    @Override
    public boolean supportsRecipe(final EmiRecipe recipe) {
        return !recipe.getInputs().isEmpty() && !recipe.getOutputs().isEmpty();
    }

    @Override
    public boolean alwaysDisplaySupport(final EmiRecipe recipe) {
        return true;
    }

    @Override
    public boolean canCraft(final EmiRecipe recipe, final EmiCraftContext<BlueprintTerminalMenu> context) {
        return true;
    }

    @Override
    public boolean craft(final EmiRecipe recipe, final EmiCraftContext<BlueprintTerminalMenu> context) {
        final BlueprintTerminalMenu menu = context.getScreenHandler();
        final BlueprintRecipeTransfer transfer = recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING
                && recipe.supportsRecipeTree()
                ? BlueprintRecipeTransfer.crafting(CraftingTerminalHandler.gridOf(recipe), menu.terminal().contents())
                : BlueprintRecipeTransfer.processing(amountsOf(recipe.getInputs()), amountsOf(recipe.getOutputs()));
        if (transfer.isEmpty()) {
            return false;
        }
        transfer.send(menu);
        Minecraft.getInstance().setScreen(context.getScreen());
        return true;
    }

    private static List<ResourceAmount> amountsOf(final List<? extends EmiIngredient> ingredients) {
        final List<ResourceAmount> amounts = new ArrayList<>(ingredients.size());
        for (EmiIngredient ingredient : ingredients) {
            final ResourceAmount amount = ingredient.isEmpty() ? null : amountOf(ingredient.getEmiStacks().getFirst());
            if (amount != null) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    private static @Nullable ResourceAmount amountOf(final EmiStack stack) {
        final int count = (int) Math.min(stack.getAmount(), Integer.MAX_VALUE);
        final ItemStack item = stack.getItemStack();
        if (!item.isEmpty()) {
            return BlueprintRecipeTransfer.amountOf(item.copyWithCount(count));
        }
        final Fluid fluid = stack.getKeyOfType(Fluid.class);
        return fluid == null ? null : BlueprintRecipeTransfer.amountOf(new FluidStack(fluid, count));
    }
}
