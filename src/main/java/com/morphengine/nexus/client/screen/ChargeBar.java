package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The thin bar that shows how full an energy buffer is, the same in every panel: a track, a fill in the colour of the
 * network and a border. What it says in numbers is written under it by the panel.
 */
final class ChargeBar {

    static final int HEIGHT = 8;

    private static final float LABEL_SCALE = 0.95F;
    private static final int LABEL_GAP = 3;

    private final int left;
    private final int top;
    private final int width;

    private ChargeBar(final int left, final int top, final int width) {
        this.left = left;
        this.top = top;
        this.width = width;
    }

    static ChargeBar at(final int left, final int top, final int width) {
        return new ChargeBar(left, top, width);
    }

    void draw(final GuiGraphicsExtractor graphics, final PanelStyle style, final long stored, final long capacity) {
        graphics.fill(left, top, left + width, top + HEIGHT, style.track());
        final int filled = capacity > 0 ? (int) ((width - 2) * stored / capacity) : 0;
        if (filled > 0) {
            graphics.fill(left + 1, top + 1, left + 1 + filled, top + HEIGHT - 1, style.accent());
        }
        graphics.outline(left, top, width, HEIGHT, style.border());
    }

    /**
     * Writes what the bar says in numbers under it, in small type.
     */
    void label(final GuiGraphicsExtractor graphics, final Font font, final String text) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, top + HEIGHT + LABEL_GAP);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
        graphics.text(font, text, 0, 0, PanelStyle.TEXT_LIGHT, false);
        graphics.pose().popMatrix();
    }
}
