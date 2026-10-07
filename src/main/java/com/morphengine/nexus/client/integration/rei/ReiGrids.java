package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.client.integration.BlueprintRecipeTransfer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the recipes REI shows: a crafting recipe placed on a 3 by 3 grid, and
 * the items and fluids of any recipe.
 */
final class ReiGrids {

    static final CategoryIdentifier<?> CRAFTING = CategoryIdentifier.of("minecraft", "plugins/crafting");
    static final int GRID_SIDE = 3;

    private ReiGrids() {
    }

    /**
     * @return whether {@code display} is a crafting recipe laid out on a grid
     */
    static boolean isCrafting(final Display display) {
        return display instanceof SimpleGridMenuDisplay && CRAFTING.equals(display.getCategoryIdentifier());
    }

    /**
     * @return the recipe's ingredients on the nine grid slots in reading order;
     *         a recipe narrower than the grid fills it from the top left corner,
     *         as on a crafting table
     */
    static List<EntryIngredient> gridOf(final SimpleGridMenuDisplay display) {
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

    static List<ItemStack> itemStacks(final EntryIngredient ingredient) {
        final List<ItemStack> stacks = new ArrayList<>(ingredient.size());
        for (EntryStack<?> entry : ingredient) {
            if (entry.getValue() instanceof ItemStack stack) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /**
     * @return the first item or fluid of each ingredient with its amount;
     *         ingredients showing neither are left out
     */
    static List<ResourceAmount> amountsOf(final List<EntryIngredient> ingredients) {
        final List<ResourceAmount> amounts = new ArrayList<>(ingredients.size());
        for (EntryIngredient ingredient : ingredients) {
            final ResourceAmount amount = ingredient.isEmpty() ? null : amountOf(ingredient.getFirst().getValue());
            if (amount != null) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    /**
     * REI hands fluids over as Architectury stacks, in millibuckets.
     */
    private static @Nullable ResourceAmount amountOf(final Object value) {
        return switch (value) {
            case ItemStack item -> BlueprintRecipeTransfer.amountOf(item);
            case dev.architectury.fluid.FluidStack fluid -> BlueprintRecipeTransfer.amountOf(
                    new FluidStack(fluid.getFluid(), (int) Math.min(fluid.getAmount(), Integer.MAX_VALUE)));
            default -> null;
        };
    }
}
