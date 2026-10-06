package com.morphengine.nexus.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * A recipe of the Extractor, type {@code nexus:extracting}: an ingredient and the fluid it gives, in millibuckets.
 *
 * @param amount millibuckets of {@code fluid} one item gives, positive
 */
public record ExtractingRecipe(Recipe.CommonInfo commonInfo, Ingredient ingredient, Fluid fluid, int amount)
        implements Recipe<SingleRecipeInput> {

    public static final MapCodec<ExtractingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(ExtractingRecipe::commonInfo),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ExtractingRecipe::ingredient),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(ExtractingRecipe::fluid),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(ExtractingRecipe::amount)
    ).apply(instance, ExtractingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, ExtractingRecipe::commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, ExtractingRecipe::ingredient,
            ByteBufCodecs.registry(Registries.FLUID), ExtractingRecipe::fluid,
            ByteBufCodecs.VAR_INT, ExtractingRecipe::amount,
            ExtractingRecipe::new);

    public static final RecipeSerializer<ExtractingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(final SingleRecipeInput input, final Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(final SingleRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ExtractingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<ExtractingRecipe> getType() {
        return NexusRecipes.EXTRACTING.get();
    }

    @Override
    public boolean showNotification() {
        return commonInfo.showNotification();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredient);
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
