package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * Colors and drawing of a device panel. Every panel of one network is tinted
 * with that network's color, so a device reads as part of its network.
 */
final class PanelStyle {

    static final int TEXT_LIGHT = 0xFFE6E6EE;
    static final int TEXT_DIM = 0xFFA0A6B4;
    static final int HEADER_HEIGHT = 16;
    /** Where the line naming the device's network sits, just under the header. */
    static final int NETWORK_TOP = HEADER_HEIGHT + 4;
    static final int PADDING = 10;
    /** The title is drawn smaller than body text, so the header stays a slim bar. */
    static final float TITLE_SCALE = 0.875F;
    static final int SLOT_SIZE = 18;
    static final int CROSS_SIZE = 6;

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
    private static final int TITLE_OFFSET_Y = 4;

    private final int accentRgb;

    private PanelStyle(final int accentRgb) {
        this.accentRgb = accentRgb;
    }

    static PanelStyle tinted(final int accentRgb) {
        return new PanelStyle(accentRgb);
    }

    /**
     * @param network the network the device belongs to; {@code null} when it
     *                belongs to none, which shows the standard blue of Nexus,
     *                as the device's model does
     */
    static PanelStyle of(final @Nullable NetworkBadge network) {
        return of(network != null ? network.color() : null);
    }

    /**
     * @param color the color of the device's network; {@code null} when it
     *              belongs to none, which shows the standard blue of Nexus
     */
    static PanelStyle of(final @Nullable NetworkColor color) {
        return tinted(color != null ? color.rgb() : NetworkColor.DEFAULT.rgb());
    }

    /**
     * Writes which network the device belongs to, or that it belongs to none,
     * at {@link #NETWORK_TOP}, where every panel shows it.
     */
    static void drawNetwork(
            final GuiGraphicsExtractor graphics, final Font font, final @Nullable NetworkBadge network,
            final int panelLeft, final int panelTop) {
        final Component line = network != null ? Component.translatable("gui.nexus.network", network.name())
                : Component.translatable("gui.nexus.no_network");
        graphics.text(font, line, panelLeft + PADDING, panelTop + NETWORK_TOP, TEXT_DIM, false);
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
        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.left() + TITLE_OFFSET_X, bounds.top() + TITLE_OFFSET_Y);
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
        graphics.text(font, title, 0, 0, TEXT_LIGHT, false);
        graphics.pose().popMatrix();
    }

    /**
     * An inventory slot whose item sits at {@code (x + 1, y + 1)}: a light well
     * with a shadow on the top left and a highlight on the bottom right, in the
     * network's border.
     */
    void drawSlot(final GuiGraphicsExtractor graphics, final int x, final int y) {
        drawSlotGrid(graphics, x, y, 1, 1);
    }

    /**
     * Slots side by side, each as {@link #drawSlot} draws it. Every line runs
     * the whole grid, so a large grid costs a few fills per row and column
     * rather than ten per slot; the pixels come out the same.
     */
    void drawSlotGrid(final GuiGraphicsExtractor graphics, final int left, final int top, final int columns,
                      final int rows) {
        final int right = left + columns * SLOT_SIZE;
        final int bottom = top + rows * SLOT_SIZE;
        final int shadow = ARGB.opaque(SLOT_SHADOW_RGB);
        final int light = ARGB.opaque(SLOT_LIGHT_RGB);
        final int border = border();
        graphics.fill(left, top, right, bottom, ARGB.opaque(blend(SLOT_BASE_RGB, SLOT_TINT)));
        for (int row = 0; row < rows; row++) {
            graphics.fill(left, top + row * SLOT_SIZE + 1, right, top + row * SLOT_SIZE + 2, shadow);
        }
        for (int column = 0; column < columns; column++) {
            graphics.fill(left + column * SLOT_SIZE + 1, top, left + column * SLOT_SIZE + 2, bottom, shadow);
        }
        for (int row = 1; row <= rows; row++) {
            graphics.fill(left, top + row * SLOT_SIZE - 2, right, top + row * SLOT_SIZE - 1, light);
        }
        for (int column = 1; column <= columns; column++) {
            graphics.fill(left + column * SLOT_SIZE - 2, top, left + column * SLOT_SIZE - 1, bottom, light);
        }
        for (int row = 0; row < rows; row++) {
            final int y = top + row * SLOT_SIZE;
            graphics.fill(left, y, right, y + 1, border);
            graphics.fill(left, y + SLOT_SIZE - 1, right, y + SLOT_SIZE, border);
        }
        for (int column = 0; column < columns; column++) {
            final int x = left + column * SLOT_SIZE;
            graphics.fill(x, top, x + 1, bottom, border);
            graphics.fill(x + SLOT_SIZE - 1, top, x + SLOT_SIZE, bottom, border);
        }
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

    /**
     * A cross of one pixel wide diagonals, {@value #CROSS_SIZE} pixels square,
     * centred in {@code area}; the area's sides must leave an even margin.
     */
    static void drawCross(final GuiGraphicsExtractor graphics, final PanelBounds area, final int color) {
        final int left = area.left() + (area.width() - CROSS_SIZE) / 2;
        final int top = area.top() + (area.height() - CROSS_SIZE) / 2;
        for (int step = 0; step < CROSS_SIZE; step++) {
            graphics.fill(left + step, top + step, left + step + 1, top + step + 1, color);
            graphics.fill(left + CROSS_SIZE - 1 - step, top + step, left + CROSS_SIZE - step, top + step + 1, color);
        }
    }

    /**
     * A small square button showing a cross, lit while the cursor is on it.
     */
    void drawCrossButton(final GuiGraphicsExtractor graphics, final PanelBounds bounds, final boolean hovered) {
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                buttonFill());
        graphics.outline(bounds.left(), bounds.top(), bounds.width(), bounds.height(),
                hovered ? TEXT_LIGHT : border());
        drawCross(graphics, bounds, hovered ? TEXT_LIGHT : TEXT_DIM);
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
