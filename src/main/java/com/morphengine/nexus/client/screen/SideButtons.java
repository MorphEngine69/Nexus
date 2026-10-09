package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * A column of square buttons standing just left of a panel, each showing an
 * icon and lit while the cursor is on it. What a button does is up to the
 * panel.
 */
final class SideButtons {

    private static final int BUTTON_SIZE = 18;
    private static final int ICON_SIZE = 16;
    private static final int SPACING = 20;
    private static final int OFFSET_LEFT = 22;
    private static final int OFFSET_TOP = 18;

    private final int left;
    private final int top;
    private final int count;

    /**
     * @param panelLeft left edge of the panel; the buttons stand just left of it
     */
    SideButtons(final int panelLeft, final int panelTop, final int count) {
        this.left = panelLeft - OFFSET_LEFT;
        this.top = panelTop + OFFSET_TOP;
        this.count = count;
    }

    PanelBounds area() {
        return new PanelBounds(left, top, BUTTON_SIZE, count * SPACING);
    }

    /**
     * @return index of the button under the cursor; -1 when there is none
     */
    int buttonAt(final double x, final double y) {
        for (int index = 0; index < count; index++) {
            if (bounds(index).contains(x, y)) {
                return index;
            }
        }
        return -1;
    }

    void draw(final GuiGraphics graphics, final PanelStyle style, final int index, final ResourceLocation icon,
              final boolean hovered) {
        final PanelBounds bounds = bounds(index);
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + BUTTON_SIZE, bounds.top() + BUTTON_SIZE,
                style.buttonFill());
        graphics.renderOutline(bounds.left(), bounds.top(), BUTTON_SIZE, BUTTON_SIZE,
                hovered ? PanelStyle.TEXT_LIGHT : style.border());
        graphics.blitSprite(icon, bounds.left() + 1, bounds.top() + 1,
                ICON_SIZE, ICON_SIZE);
    }

    private PanelBounds bounds(final int index) {
        return new PanelBounds(left, top + index * SPACING, BUTTON_SIZE, BUTTON_SIZE);
    }
}
