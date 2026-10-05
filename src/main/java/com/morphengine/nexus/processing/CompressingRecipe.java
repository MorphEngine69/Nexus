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
 * A recipe of the Compressor: {@code ingredient} and {@code result}, in the type {@code nexus:compressing}.
 */
public final class CompressingRecipe extends ProcessingRecipe {

    public static final MapCodec<CompressingRecipe> MAP_CODEC = simpleMapCodec(CompressingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompressingRecipe> STREAM_CODEC =
            simpleStreamCodec(CompressingRecipe::new);
    public static final RecipeSerializer<CompressingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CompressingRecipe(
            final Recipe.CommonInfo commonInfo, final Ingredient ingredient, final ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    @Override
    public RecipeType<CompressingRecipe> getType() {
        return NexusRecipes.COMPRESSING.get();
    }

    @Override
    public RecipeSerializer<CompressingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
