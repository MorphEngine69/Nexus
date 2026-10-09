package com.morphengine.nexus.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * A recipe of the Extractor, type {@code nexus:extracting}: an ingredient and the fluid it gives, in millibuckets.
 *
 * @param amount millibuckets of {@code fluid} one item gives, positive
 */
public record ExtractingRecipe(Ingredient ingredient, Fluid fluid, int amount) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<ExtractingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ExtractingRecipe::ingredient),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(ExtractingRecipe::fluid),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(ExtractingRecipe::amount)
    ).apply(instance, ExtractingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ExtractingRecipe::ingredient,
            ByteBufCodecs.registry(Registries.FLUID), ExtractingRecipe::fluid,
            ByteBufCodecs.VAR_INT, ExtractingRecipe::amount,
            ExtractingRecipe::new);

    public static final RecipeSerializer<ExtractingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<ExtractingRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtractingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    @Override
    public boolean matches(final SingleRecipeInput input, final Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(final SingleRecipeInput input, final HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(final HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        final NonNullList<Ingredient> all = NonNullList.create();
        all.add(ingredient);
        return all;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<ExtractingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<ExtractingRecipe> getType() {
        return NexusRecipes.EXTRACTING.get();
    }
}
