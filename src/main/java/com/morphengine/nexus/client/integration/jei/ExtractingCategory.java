package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.processing.ExtractingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * The category of the Extractor: the item, an arrow and the fluid it gives, in millibuckets.
 */
final class ExtractingCategory extends AbstractRecipeCategory<RecipeHolder<ExtractingRecipe>> {

    private static final int WIDTH = 96;
    private static final int HEIGHT = 28;
    private static final int INPUT_X = 8;
    private static final int OUTPUT_X = 72;
    private static final int SLOT_Y = 5;
    private static final int ARROW_X = 36;
    private static final int ARROW_TICKS = 60;
    private static final int TANK_SIZE = 16;

    private final IDrawable arrow;

    ExtractingCategory(final IGuiHelper helper, final String titleKey, final ItemLike machine) {
        super(MachineRecipeTypes.EXTRACTING, Component.translatable(titleKey), helper.createDrawableItemLike(machine),
                WIDTH, HEIGHT);
        this.arrow = helper.createAnimatedRecipeArrow(ARROW_TICKS);
    }

    @Override
    public void setRecipe(
            final IRecipeLayoutBuilder builder, final RecipeHolder<ExtractingRecipe> holder,
            final IFocusGroup focuses) {
        final ExtractingRecipe recipe = holder.value();
        builder.addInputSlot(INPUT_X, SLOT_Y).setStandardSlotBackground().add(recipe.ingredient());
        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .setFluidRenderer(FluidType.BUCKET_VOLUME, false, TANK_SIZE, TANK_SIZE)
                .add(recipe.fluid(), recipe.amount());
    }

    @Override
    public void draw(
            final RecipeHolder<ExtractingRecipe> holder, final IRecipeSlotsView slots,
            final GuiGraphicsExtractor graphics, final double mouseX, final double mouseY) {
        arrow.draw(graphics, ARROW_X, SLOT_Y);
    }
}
