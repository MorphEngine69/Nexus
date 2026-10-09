package com.morphengine.nexus.processing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A recipe of a machine that turns one item into another, such as a Crusher: an ingredient and a result, as in a
 * stonecutter. The time of the work is up to the machine. Machine recipes are not unlocked or shown in the recipe book.
 */
public abstract class ProcessingRecipe extends SingleItemRecipe {

    ProcessingRecipe(
            final RecipeType<?> type, final RecipeSerializer<?> serializer, final String group,
            final Ingredient ingredient, final ItemStack result) {
        super(type, serializer, group, ingredient, result);
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStack result() {
        return result;
    }

    @Override
    public boolean matches(final SingleRecipeInput input, final Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
