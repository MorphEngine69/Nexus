package com.morphengine.nexus.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
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
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            AlloyIngredient.CODEC.listOf(1, MOST_INPUTS).fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(instance, AlloyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            AlloyIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyingRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
            AlloyingRecipe::new);

    public static final RecipeSerializer<AlloyingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Recipe.CommonInfo commonInfo;
    private final List<AlloyIngredient> ingredients;
    private final ItemStackTemplate result;

    public AlloyingRecipe(
            final Recipe.CommonInfo commonInfo, final List<AlloyIngredient> ingredients,
            final ItemStackTemplate result) {
        this.commonInfo = commonInfo;
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
        return result.create();
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
    public ItemStack assemble(final AlloyInput input) {
        return result.create();
    }

    @Override
    public RecipeSerializer<AlloyingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<AlloyingRecipe> getType() {
        return NexusRecipes.ALLOYING.get();
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
        return PlacementInfo.create(ingredients.stream().map(AlloyIngredient::ingredient).toList());
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
