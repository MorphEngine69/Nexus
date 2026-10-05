package com.morphengine.nexus.processing;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.List;

/**
 * A recipe of a machine that turns one item into another, such as a Crusher: an ingredient and a result, as in a
 * stonecutter. The time of the work is up to the machine. Machine recipes are not unlocked or shown in the recipe book.
 */
abstract class ProcessingRecipe extends SingleItemRecipe {

    ProcessingRecipe(final Recipe.CommonInfo commonInfo, final Ingredient ingredient, final ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.STONECUTTER;
    }
}
