package com.morphengine.nexus.processing;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A recipe of the Pulverizer: {@code ingredient} and {@code result}, in the type {@code nexus:pulverizing}.
 */
public final class PulverizingRecipe extends ProcessingRecipe {

    public static final MapCodec<PulverizingRecipe> MAP_CODEC = simpleMapCodec(PulverizingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PulverizingRecipe> STREAM_CODEC =
            simpleStreamCodec(PulverizingRecipe::new);
    public static final RecipeSerializer<PulverizingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public PulverizingRecipe(
            final Recipe.CommonInfo commonInfo, final Ingredient ingredient, final ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    @Override
    public RecipeType<PulverizingRecipe> getType() {
        return NexusRecipes.PULVERIZING.get();
    }

    @Override
    public RecipeSerializer<PulverizingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
