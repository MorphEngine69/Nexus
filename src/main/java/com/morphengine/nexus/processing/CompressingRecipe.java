package com.morphengine.nexus.processing;

import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * A recipe of the Compressor: {@code ingredient} and {@code result}, in the type {@code nexus:compressing}.
 */
public final class CompressingRecipe extends ProcessingRecipe {

    public static final RecipeSerializer<CompressingRecipe> SERIALIZER =
            new SingleItemRecipe.Serializer<CompressingRecipe>(CompressingRecipe::new) {
            };

    public CompressingRecipe(final String group, final Ingredient ingredient, final ItemStack result) {
        super(NexusRecipes.COMPRESSING.get(), SERIALIZER, group, ingredient, result);
    }

    @Override
    public RecipeType<CompressingRecipe> getType() {
        return NexusRecipes.COMPRESSING.get();
    }
}
