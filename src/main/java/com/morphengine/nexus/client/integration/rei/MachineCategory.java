package com.morphengine.nexus.client.integration.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * The category of a machine: its ingredients in a row, an arrow and what comes out, drawn the same for the Crusher, the
 * Pulverizer, the Compressor, the Alloy Smelter and the Extractor, which differ in how many slots there are and in
 * whether the result is an item or a fluid.
 */
final class MachineCategory implements DisplayCategory<MachineDisplay> {

    private static final int HEIGHT = 36;
    private static final int WIDTH_PADDING = 12;
    private static final int SLOT_PITCH = 20;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_GAP = 6;
    private static final int OUTPUT_WIDTH = 26;
    private static final int OUTPUT_PADDING = 5;
    private static final int ARROW_TICKS = 40;
    private static final int SLOT_TOP = 9;
    private static final int ARROW_TOP = 9;
    private static final int SLOT_SIZE = 16;

    private final CategoryIdentifier<MachineDisplay> id;
    private final Component title;
    private final ItemLike machine;

    MachineCategory(final CategoryIdentifier<MachineDisplay> id, final String titleKey, final ItemLike machine) {
        this.id = id;
        this.title = Component.translatable(titleKey);
        this.machine = machine;
    }

    @Override
    public CategoryIdentifier<? extends MachineDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(machine);
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }

    @Override
    public int getDisplayWidth(final MachineDisplay display) {
        return WIDTH_PADDING * 2 + display.getInputEntries().size() * SLOT_PITCH + ARROW_GAP + ARROW_WIDTH
                + OUTPUT_WIDTH;
    }

    @Override
    public List<Widget> setupDisplay(final MachineDisplay display, final Rectangle bounds) {
        final List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        int x = bounds.x + WIDTH_PADDING;
        final int top = bounds.y + SLOT_TOP;
        for (EntryIngredient input : display.getInputEntries()) {
            widgets.add(Widgets.createSlot(new Point(x, top)).entries(input).markInput());
            x += SLOT_PITCH;
        }
        x += ARROW_GAP;
        widgets.add(Widgets.createArrow(new Point(x, bounds.y + ARROW_TOP)).animationDurationTicks(ARROW_TICKS));
        x += ARROW_WIDTH;
        widgets.add(Widgets.createResultSlotBackground(new Point(x + OUTPUT_PADDING, top)));
        widgets.add(Widgets.createSlot(new Point(x + OUTPUT_PADDING, top))
                .entries(display.getOutputEntries().getFirst()).disableBackground().markOutput());
        return widgets;
    }
}
