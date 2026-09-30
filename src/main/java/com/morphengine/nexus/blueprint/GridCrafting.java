package com.morphengine.nexus.blueprint;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a crafting table makes of a 3 by 3 grid, worked out with the recipes of
 * the world. Server side only.
 */
public final class GridCrafting {

    private GridCrafting() {
    }

    /**
     * @param grid one stack per slot, row by row, {@link GridSlot#COUNT} in all
     * @return the result, then what the ingredients leave behind, each item
     *         once; empty when no recipe matches or it gives nothing
     */
    public static List<ResourceAmount> outputsOf(final ServerLevel level, final List<ItemStack> grid) {
        final CraftingInput input = inputOf(grid);
        final Optional<RecipeHolder<CraftingRecipe>> recipe = recipeFor(level, input);
        if (recipe.isEmpty()) {
            return List.of();
        }
        final ItemStack result = recipe.get().value().assemble(input);
        if (result.isEmpty() || !result.isItemEnabled(level.enabledFeatures())) {
            return List.of();
        }
        final List<ResourceAmount> outputs = new ArrayList<>();
        outputs.add(new ResourceAmount(ItemKey.of(result), result.getCount()));
        for (ItemStack remainder : recipe.get().value().getRemainingItems(input)) {
            if (!remainder.isEmpty()) {
                outputs.add(new ResourceAmount(ItemKey.of(remainder), remainder.getCount()));
            }
        }
        return outputs;
    }

    /**
     * @param grid one stack per slot, row by row, {@link GridSlot#COUNT} in all
     * @return what the grid crafts and what its recipe accepts in each slot
     */
    public static GridRecipe recipeOf(final ServerLevel level, final List<ItemStack> grid) {
        final List<ResourceAmount> outputs = outputsOf(level, grid);
        return outputs.isEmpty() ? GridRecipe.NONE : new GridRecipe(outputs, acceptedBy(level, grid));
    }

    /**
     * What the recipe the grid matches accepts in each filled slot: of its
     * ingredients that accept the item lying there, the one accepting the
     * fewest items, so a slot never takes what the recipe would not take
     * there.
     *
     * @param grid one stack per slot, row by row, {@link GridSlot#COUNT} in all
     * @return per filled slot index, what the recipe accepts there; empty when
     *         no recipe matches
     */
    private static Map<Integer, Ingredient> acceptedBy(final ServerLevel level, final List<ItemStack> grid) {
        final Optional<RecipeHolder<CraftingRecipe>> recipe = recipeFor(level, inputOf(grid));
        if (recipe.isEmpty()) {
            return Map.of();
        }
        final List<Ingredient> ingredients = recipe.get().value().placementInfo().ingredients();
        final Map<Integer, Ingredient> accepted = new HashMap<>();
        for (int slot = 0; slot < grid.size(); slot++) {
            final Ingredient narrowest = narrowestAccepting(ingredients, grid.get(slot));
            if (narrowest != null) {
                accepted.put(slot, narrowest);
            }
        }
        return Map.copyOf(accepted);
    }

    private static @Nullable Ingredient narrowestAccepting(final List<Ingredient> ingredients,
                                                           final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Ingredient narrowest = null;
        long narrowestSize = Long.MAX_VALUE;
        for (Ingredient ingredient : ingredients) {
            final long size = ingredient.items().count();
            if (ingredient.test(stack) && size < narrowestSize) {
                narrowest = ingredient;
                narrowestSize = size;
            }
        }
        return narrowest;
    }

    /**
     * @param picked the items picked for one run of {@code blueprint}
     * @return whether the grid laid out with {@code picked} crafts what the
     *         blueprint was encoded with; a changed recipe or an item the
     *         recipe does not take must not quietly give something else
     */
    public static boolean crafts(final ServerLevel level, final CraftingBlueprint blueprint,
                                 final List<ResourceAmount> picked) {
        final List<ItemStack> grid = blueprint.stacksOf(picked);
        return !grid.isEmpty() && outputsOf(level, grid).equals(blueprint.outputs());
    }

    private static CraftingInput inputOf(final List<ItemStack> grid) {
        return CraftingInput.of(GridSlot.SIDE, GridSlot.SIDE, grid);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> recipeFor(final ServerLevel level,
                                                                     final CraftingInput input) {
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }
}
