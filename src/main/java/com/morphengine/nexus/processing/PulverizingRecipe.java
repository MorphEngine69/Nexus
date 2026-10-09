package com.morphengine.nexus.processing;

import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * A recipe of the Pulverizer: {@code ingredient} and {@code result}, in the type {@code nexus:pulverizing}.
 */
public final class PulverizingRecipe extends ProcessingRecipe {

    public static final RecipeSerializer<PulverizingRecipe> SERIALIZER =
            new SingleItemRecipe.Serializer<PulverizingRecipe>(PulverizingRecipe::new) {
            };

    public PulverizingRecipe(final String group, final Ingredient ingredient, final ItemStack result) {
        super(NexusRecipes.PULVERIZING.get(), SERIALIZER, group, ingredient, result);
    }

    @Override
    public RecipeType<PulverizingRecipe> getType() {
        return NexusRecipes.PULVERIZING.get();
    }
}
