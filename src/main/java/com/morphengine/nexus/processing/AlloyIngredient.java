package com.morphengine.nexus.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One input of an alloying recipe: what it accepts and how many.
 *
 * @param count how many items the recipe uses up of it, positive
 */
public record AlloyIngredient(Ingredient ingredient, int count) {

    public static final Codec<AlloyIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(AlloyIngredient::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(AlloyIngredient::count)
    ).apply(instance, AlloyIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AlloyIngredient::ingredient,
            ByteBufCodecs.VAR_INT, AlloyIngredient::count,
            AlloyIngredient::new);
}
