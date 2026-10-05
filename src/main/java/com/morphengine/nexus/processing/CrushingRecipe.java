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
 * A recipe of the Crusher: {@code ingredient} and {@code result}, in the type {@code nexus:crushing}.
 */
public final class CrushingRecipe extends ProcessingRecipe {

    public static final MapCodec<CrushingRecipe> MAP_CODEC = simpleMapCodec(CrushingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC =
            simpleStreamCodec(CrushingRecipe::new);
    public static final RecipeSerializer<CrushingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CrushingRecipe(
            final Recipe.CommonInfo commonInfo, final Ingredient ingredient, final ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return NexusRecipes.CRUSHING.get();
    }

    @Override
    public RecipeSerializer<CrushingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
