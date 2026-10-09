package com.morphengine.nexus.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe of the Alloy Smelter, type {@code nexus:alloying}: one to three inputs, each with an ingredient and a count,
 * and a result. Each input is met by an item of its own.
 */
public final class AlloyingRecipe implements Recipe<AlloyInput> {

    private static final int MOST_INPUTS = 3;

    public static final MapCodec<AlloyingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AlloyIngredient.CODEC.listOf(1, MOST_INPUTS).fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(instance, AlloyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            AlloyIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyingRecipe::ingredients,
            ItemStack.STREAM_CODEC, recipe -> recipe.result,
            AlloyingRecipe::new);

    public static final RecipeSerializer<AlloyingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<AlloyingRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final List<AlloyIngredient> ingredients;
    private final ItemStack result;

    public AlloyingRecipe(final List<AlloyIngredient> ingredients, final ItemStack result) {
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
    }

    public List<AlloyIngredient> ingredients() {
        return ingredients;
    }

    /**
     * @return a stack of what one run of the recipe makes
     */
    public ItemStack resultStack() {
        return result.copy();
    }

    /**
     * @return for each ingredient, in order, the stack that meets it with the count the recipe uses; empty when the
     *         input does not hold enough for every ingredient, each in an item of its own
     */
    public List<ItemStack> pick(final AlloyInput input) {
        final boolean[] taken = new boolean[input.size()];
        final List<ItemStack> picked = new ArrayList<>(ingredients.size());
        for (AlloyIngredient needed : ingredients) {
            final int found = firstMeeting(input, taken, needed);
            if (found < 0) {
                return List.of();
            }
            taken[found] = true;
            picked.add(input.getItem(found).copyWithCount(needed.count()));
        }
        return picked;
    }

    private static int firstMeeting(final AlloyInput input, final boolean[] taken, final AlloyIngredient needed) {
        for (int index = 0; index < input.size(); index++) {
            final ItemStack stack = input.getItem(index);
            if (!taken[index] && stack.getCount() >= needed.count() && needed.ingredient().test(stack)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * @return whether some ingredient of the recipe accepts the stack
     */
    public boolean usesItem(final ItemStack stack) {
        return ingredients.stream().anyMatch(needed -> needed.ingredient().test(stack));
    }

    @Override
    public boolean matches(final AlloyInput input, final Level level) {
        return !pick(input).isEmpty();
    }

    @Override
    public ItemStack assemble(final AlloyInput input, final HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(final HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        final NonNullList<Ingredient> all = NonNullList.create();
        ingredients.forEach(needed -> all.add(needed.ingredient()));
        return all;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<AlloyingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<AlloyingRecipe> getType() {
        return NexusRecipes.ALLOYING.get();
    }
}
