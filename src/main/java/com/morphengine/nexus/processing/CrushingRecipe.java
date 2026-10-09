package com.morphengine.nexus.processing;

import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * A recipe of the Crusher: {@code ingredient} and {@code result}, in the type {@code nexus:crushing}.
 */
public final class CrushingRecipe extends ProcessingRecipe {

    public static final RecipeSerializer<CrushingRecipe> SERIALIZER =
            new SingleItemRecipe.Serializer<CrushingRecipe>(CrushingRecipe::new) {
            };

    public CrushingRecipe(final String group, final Ingredient ingredient, final ItemStack result) {
        super(NexusRecipes.CRUSHING.get(), SERIALIZER, group, ingredient, result);
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return NexusRecipes.CRUSHING.get();
    }
}
