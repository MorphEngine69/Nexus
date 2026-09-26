package com.morphengine.nexus.client.integration.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Some ingredients of a recipe are nowhere to be found: their slots are marked
 * in red and the button turns orange, but the rest can still be laid out.
 *
 * @param slots the recipe's slots whose ingredient is missing
 */
record MissingIngredientsError(List<IRecipeSlotView> slots) implements IRecipeTransferError {

    private static final int SLOT_HIGHLIGHT = 0x66FF0000;
    private static final int BUTTON_HIGHLIGHT = 0x80FFA500;

    MissingIngredientsError {
        slots = List.copyOf(slots);
    }

    @Override
    public Type getType() {
        return Type.COSMETIC;
    }

    @Override
    public int getButtonHighlightColor() {
        return BUTTON_HIGHLIGHT;
    }

    @Override
    public void showError(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                          final IRecipeSlotsView recipeSlots, final int recipeX, final int recipeY) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(recipeX, recipeY);
        for (IRecipeSlotView slot : slots) {
            slot.drawHighlight(graphics, SLOT_HIGHLIGHT);
        }
        graphics.pose().popMatrix();
    }

    @Override
    public void getTooltip(final ITooltipBuilder tooltip) {
        tooltip.add(Component.translatable("gui.nexus.recipe_transfer.missing"));
    }

    @Override
    public int getMissingCountHint() {
        return slots.size();
    }
}
