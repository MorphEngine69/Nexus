package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.processing.AlloyIngredient;
import com.morphengine.nexus.processing.AlloyingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.List;

/**
 * The category of the Alloy Smelter: up to three ingredients, each with the count the recipe takes, an arrow and the
 * alloy.
 */
final class AlloyingCategory extends AbstractRecipeCategory<RecipeHolder<AlloyingRecipe>> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 28;
    private static final int INPUT_X = 4;
    private static final int SLOT_PITCH = 20;
    private static final int SLOT_Y = 5;
    private static final int ARROW_X = 68;
    private static final int OUTPUT_X = 98;
    private static final int ARROW_TICKS = 60;

    private final IDrawable arrow;

    AlloyingCategory(final IGuiHelper helper, final String titleKey, final ItemLike machine) {
        super(MachineRecipeTypes.ALLOYING, Component.translatable(titleKey), helper.createDrawableItemLike(machine),
                WIDTH, HEIGHT);
        this.arrow = helper.createAnimatedRecipeArrow(ARROW_TICKS);
    }

    @Override
    public void setRecipe(
            final IRecipeLayoutBuilder builder, final RecipeHolder<AlloyingRecipe> holder, final IFocusGroup focuses) {
        final AlloyingRecipe recipe = holder.value();
        int slot = 0;
        for (AlloyIngredient needed : recipe.ingredients()) {
            builder.addInputSlot(INPUT_X + slot * SLOT_PITCH, SLOT_Y).setStandardSlotBackground()
                    .addItemStacks(stacksOf(needed));
            slot++;
        }
        builder.addOutputSlot(OUTPUT_X, SLOT_Y).setOutputSlotBackground().addItemStack(recipe.resultStack());
    }

    private static List<ItemStack> stacksOf(final AlloyIngredient needed) {
        return Arrays.stream(needed.ingredient().getItems()).map(item -> item.copyWithCount(needed.count())).toList();
    }

    @Override
    public void draw(
            final RecipeHolder<AlloyingRecipe> holder, final IRecipeSlotsView slots,
            final GuiGraphics graphics, final double mouseX, final double mouseY) {
        arrow.draw(graphics, ARROW_X, SLOT_Y);
    }
}
