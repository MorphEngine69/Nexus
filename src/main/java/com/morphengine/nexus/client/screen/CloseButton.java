package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The cross in the top right corner of a device panel.
 */
final class CloseButton {

    private static final int SIZE = 10;
    private static final int MARGIN = 3;
    private static final int GLYPH_RADIUS = 2;

    private final PanelBounds bounds;

    private CloseButton(final PanelBounds bounds) {
        this.bounds = bounds;
    }

    static CloseButton inHeaderOf(final PanelBounds panel) {
        final int x = panel.left() + panel.width() - MARGIN - SIZE;
        final int y = panel.top() + (PanelStyle.HEADER_HEIGHT - SIZE) / 2;
        return new CloseButton(new PanelBounds(x, y, SIZE, SIZE));
    }

    boolean contains(final double x, final double y) {
        return bounds.contains(x, y);
    }

    void draw(final GuiGraphics graphics) {
        final int centerX = bounds.left() + SIZE / 2;
        final int centerY = bounds.top() + SIZE / 2;
        for (int offset = -GLYPH_RADIUS; offset <= GLYPH_RADIUS; offset++) {
            graphics.fill(centerX + offset, centerY + offset, centerX + offset + 1, centerY + offset + 1,
                    PanelStyle.TEXT_LIGHT);
            graphics.fill(centerX + offset, centerY - offset, centerX + offset + 1, centerY - offset + 1,
                    PanelStyle.TEXT_LIGHT);
        }
    }
}
