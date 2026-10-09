package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.machine.InputMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * The button of the input mode of a machine, a square standing just left of its panel: it shows what the mode does,
 * one line for each resource or one resource spread over all the lines, and a click switches the mode. What the click
 * does is the business of the server, which the button reaches as a menu button.
 */
final class InputModeButton {

    private static final int SIZE = 18;
    private static final int OFFSET_LEFT = 22;
    private static final int OFFSET_TOP = 38;
    private static final int DOT = 4;
    private static final int MARGIN = 2;
    private static final int ROW_GAP = 5;
    private static final int LINE_START = 9;
    private static final int LINE_THICKNESS = 2;
    private static final int SPREAD_LINES = 3;
    private static final int SHADE_DIM = 0xFF6B6F7A;

    private final PanelBounds bounds;
    private final int buttonId;

    /**
     * @param panel    the panel; the button stands just left of it
     * @param buttonId the menu button id that switches the mode
     */
    InputModeButton(final PanelBounds panel, final int buttonId) {
        this.bounds = new PanelBounds(panel.left() - OFFSET_LEFT, panel.top() + OFFSET_TOP, SIZE, SIZE);
        this.buttonId = buttonId;
    }

    Rect2i area() {
        return bounds.toRect();
    }

    void draw(
            final GuiGraphics graphics, final PanelStyle style, final InputMode mode, final int mouseX,
            final int mouseY) {
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + SIZE, bounds.top() + SIZE, style.buttonFill());
        graphics.renderOutline(bounds.left(), bounds.top(), SIZE, SIZE,
                bounds.contains(mouseX, mouseY) ? PanelStyle.TEXT_LIGHT : style.border());
        if (mode == InputMode.PER_RESOURCE) {
            drawSeparateLines(graphics, style);
        } else {
            drawSpread(graphics, style);
        }
    }

    boolean click(final Minecraft minecraft, final int containerId, final double x, final double y) {
        if (!bounds.contains(x, y) || minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(containerId, buttonId);
        return true;
    }

    void showTooltip(
            final GuiGraphics graphics, final Font font, final InputMode mode, final int mouseX,
            final int mouseY) {
        if (bounds.contains(mouseX, mouseY)) {
            final String key = mode.name().toLowerCase(Locale.ROOT);
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.nexus.machine.mode", Component.translatable(
                            "gui.nexus.machine.mode." + key)),
                    Component.translatable("gui.nexus.machine.mode." + key + ".hint")), mouseX, mouseY);
        }
    }

    /** Three dots, each with a line of its own to the right: every resource has a line to itself. */
    private void drawSeparateLines(final GuiGraphics graphics, final PanelStyle style) {
        final int[] shades = {style.accent(), PanelStyle.TEXT_LIGHT, SHADE_DIM};
        for (int row = 0; row < shades.length; row++) {
            final int y = rowTop(row);
            graphics.fill(bounds.left() + MARGIN, y, bounds.left() + MARGIN + DOT, y + DOT, shades[row]);
            graphics.fill(bounds.left() + LINE_START, y + 1, bounds.left() + SIZE - MARGIN, y + 1 + LINE_THICKNESS,
                    shades[row]);
        }
    }

    /** One dot on the left and a fork to three lines on the right: one resource goes to every line. */
    private void drawSpread(final GuiGraphics graphics, final PanelStyle style) {
        final int color = style.accent();
        final int middle = rowTop(1);
        graphics.fill(bounds.left() + MARGIN, middle, bounds.left() + MARGIN + DOT, middle + DOT, color);
        graphics.fill(bounds.left() + MARGIN + DOT, middle + 1, bounds.left() + LINE_START, middle + 1 + LINE_THICKNESS,
                color);
        graphics.fill(bounds.left() + LINE_START, rowTop(0) + 1, bounds.left() + LINE_START + LINE_THICKNESS,
                rowTop(SPREAD_LINES - 1) + 1 + LINE_THICKNESS, color);
        for (int row = 0; row < SPREAD_LINES; row++) {
            graphics.fill(bounds.left() + LINE_START, rowTop(row) + 1, bounds.left() + SIZE - MARGIN,
                    rowTop(row) + 1 + LINE_THICKNESS, color);
        }
    }

    private int rowTop(final int row) {
        return bounds.top() + MARGIN + row * ROW_GAP;
    }
}
