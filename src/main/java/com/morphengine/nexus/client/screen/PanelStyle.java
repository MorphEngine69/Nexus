package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * Colors and drawing of a device panel. Every panel of one network is tinted
 * with that network's color, so a device reads as part of its network.
 */
final class PanelStyle {

    static final int TEXT_LIGHT = 0xFFE6E6EE;
    static final int TEXT_DIM = 0xFFA0A6B4;
    static final int HEADER_HEIGHT = 20;
    static final int SLOT_SIZE = 18;

    private static final int NEUTRAL_ACCENT_RGB = 0x2C2D32;
    private static final int PANEL_BASE_RGB = 0x16171E;
    private static final int PANEL_BORDER_RGB = 0x2C2D32;
    private static final int BUTTON_FILL_RGB = 0x20222A;
    private static final int HEADER_BASE_RGB = 0x24252C;
    private static final int TRACK_BASE_RGB = 0x0E0E14;
    /** Light enough that dark items such as a coal block stand out on it. */
    private static final int SLOT_BASE_RGB = 0x33353D;
    private static final int SLOT_SHADOW_RGB = 0x1C1D23;
    private static final int SLOT_LIGHT_RGB = 0x474953;
    private static final float SLOT_TINT = 0.12F;
    private static final float PANEL_TINT = 0.10F;
    private static final float BORDER_TINT = 0.55F;
    private static final float BUTTON_TINT = 0.22F;
    private static final float HEADER_TINT = 0.40F;
    private static final int INTERIOR_ALPHA = 0xC0000000;
    private static final int TITLE_OFFSET_X = 8;
    private static final int TITLE_OFFSET_Y = 6;

    private final int accentRgb;

    private PanelStyle(final int accentRgb) {
        this.accentRgb = accentRgb;
    }

    static PanelStyle tinted(final int accentRgb) {
        return new PanelStyle(accentRgb);
    }

    /**
     * For a device that is not in any network.
     */
    static PanelStyle neutral() {
        return new PanelStyle(NEUTRAL_ACCENT_RGB);
    }

    int accent() {
        return ARGB.opaque(accentRgb);
    }

    int border() {
        return ARGB.opaque(blend(PANEL_BORDER_RGB, BORDER_TINT));
    }

    int buttonFill() {
        return INTERIOR_ALPHA | blend(BUTTON_FILL_RGB, BUTTON_TINT);
    }

    int track() {
        return ARGB.opaque(TRACK_BASE_RGB);
    }

    /**
     * Panel background, border and header bar with the title in it.
     */
    void drawFrame(
            final GuiGraphicsExtractor graphics, final Font font, final PanelBounds bounds, final Component title) {
        final int right = bounds.left() + bounds.width();
        graphics.fill(bounds.left(), bounds.top(), right, bounds.top() + bounds.height(),
                INTERIOR_ALPHA | blend(PANEL_BASE_RGB, PANEL_TINT));
        graphics.outline(bounds.left(), bounds.top(), bounds.width(), bounds.height(), border());
        graphics.fill(bounds.left(), bounds.top(), right, bounds.top() + HEADER_HEIGHT,
                INTERIOR_ALPHA | blend(HEADER_BASE_RGB, HEADER_TINT));
        graphics.text(font, title, bounds.left() + TITLE_OFFSET_X, bounds.top() + TITLE_OFFSET_Y, TEXT_LIGHT, true);
    }

    /**
     * An inventory slot whose item sits at {@code (x + 1, y + 1)}: a light well
     * with a shadow on the top left and a highlight on the bottom right, in the
     * network's border.
     */
    void drawSlot(final GuiGraphicsExtractor graphics, final int x, final int y) {
        final int right = x + SLOT_SIZE;
        final int bottom = y + SLOT_SIZE;
        graphics.fill(x, y, right, bottom, ARGB.opaque(blend(SLOT_BASE_RGB, SLOT_TINT)));
        graphics.fill(x + 1, y + 1, right - 1, y + 2, ARGB.opaque(SLOT_SHADOW_RGB));
        graphics.fill(x + 1, y + 1, x + 2, bottom - 1, ARGB.opaque(SLOT_SHADOW_RGB));
        graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, ARGB.opaque(SLOT_LIGHT_RGB));
        graphics.fill(right - 2, y + 1, right - 1, bottom - 1, ARGB.opaque(SLOT_LIGHT_RGB));
        graphics.outline(x, y, SLOT_SIZE, SLOT_SIZE, border());
    }

    void drawButton(
            final GuiGraphicsExtractor graphics, final Font font, final PanelBounds bounds, final Component label) {
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                buttonFill());
        graphics.outline(bounds.left(), bounds.top(), bounds.width(), bounds.height(), border());
        final int textX = bounds.left() + (bounds.width() - font.width(label)) / 2;
        final int textY = bounds.top() + (bounds.height() - font.lineHeight) / 2 + 1;
        graphics.text(font, label, textX, textY, TEXT_LIGHT, false);
    }

    private int blend(final int baseRgb, final float weight) {
        final int red = mix(ARGB.red(baseRgb), ARGB.red(accentRgb), weight);
        final int green = mix(ARGB.green(baseRgb), ARGB.green(accentRgb), weight);
        final int blue = mix(ARGB.blue(baseRgb), ARGB.blue(accentRgb), weight);
        return ARGB.color(0, red, green, blue);
    }

    private static int mix(final int base, final int accent, final float weight) {
        return Math.round(base + (accent - base) * weight);
    }
}
