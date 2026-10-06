package com.morphengine.nexus.client.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;

/**
 * The category of a machine that turns one item into another, the Crusher, the Pulverizer or the Compressor: the item,
 * an arrow and the result.
 *
 * @param <R> the recipe of the machine
 */
final class ItemProcessingCategory<R extends SingleItemRecipe> extends AbstractRecipeCategory<RecipeHolder<R>> {

    private static final int WIDTH = 96;
    private static final int HEIGHT = 28;
    private static final int INPUT_X = 8;
    private static final int OUTPUT_X = 72;
    private static final int SLOT_Y = 5;
    private static final int ARROW_X = 36;
    private static final int ARROW_TICKS = 40;

    private final IDrawable arrow;

    /**
     * @param titleKey the key of the name of the category
     * @param machine  the machine, which the category shows as its icon
     */
    ItemProcessingCategory(
            final IGuiHelper helper, final IRecipeHolderType<R> type, final String titleKey, final ItemLike machine) {
        super(type, Component.translatable(titleKey), helper.createDrawableItemLike(machine), WIDTH, HEIGHT);
        this.arrow = helper.createAnimatedRecipeArrow(ARROW_TICKS);
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final RecipeHolder<R> holder, final IFocusGroup focuses) {
        final R recipe = holder.value();
        builder.addInputSlot(INPUT_X, SLOT_Y).setStandardSlotBackground().add(recipe.input());
        final ItemStack result = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        builder.addOutputSlot(OUTPUT_X, SLOT_Y).setOutputSlotBackground().add(result);
    }

    @Override
    public void draw(
            final RecipeHolder<R> holder, final IRecipeSlotsView slots, final GuiGraphicsExtractor graphics,
            final double mouseX, final double mouseY) {
        arrow.draw(graphics, ARROW_X, SLOT_Y);
    }
}
