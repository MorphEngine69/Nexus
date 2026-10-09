package com.morphengine.nexus.client.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One recipe of a machine as EMI shows it: its ingredients in a row, an arrow and what comes out, drawn the same for
 * every machine, which differ in how many slots there are and in whether the result is an item or a fluid.
 */
final class MachineEmiRecipe extends BasicEmiRecipe {

    private static final int HEIGHT = 28;
    private static final int PADDING = 4;
    private static final int SLOT_PITCH = 20;
    private static final int SLOT_SIZE = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_GAP = 6;
    private static final int SLOT_TOP = 5;
    private static final int ARROW_TOP = 6;
    private static final int ARROW_TICKS = 6000;

    MachineEmiRecipe(
            final EmiRecipeCategory category, final ResourceLocation id, final List<EmiIngredient> ingredients,
            final EmiStack result) {
        super(category, id, widthFor(ingredients.size()), HEIGHT);
        this.inputs.addAll(ingredients);
        this.outputs.add(result);
    }

    private static int widthFor(final int ingredients) {
        return PADDING * 2 + ingredients * SLOT_PITCH + ARROW_GAP + ARROW_WIDTH + SLOT_SIZE + ARROW_GAP;
    }

    @Override
    public void addWidgets(final WidgetHolder widgets) {
        int x = PADDING;
        for (EmiIngredient input : inputs) {
            widgets.addSlot(input, x, SLOT_TOP);
            x += SLOT_PITCH;
        }
        x += ARROW_GAP;
        widgets.addFillingArrow(x, ARROW_TOP, ARROW_TICKS);
        x += ARROW_WIDTH + ARROW_GAP;
        widgets.addSlot(outputs.getFirst(), x, SLOT_TOP).recipeContext(this);
    }
}
