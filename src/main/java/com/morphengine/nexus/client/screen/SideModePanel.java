package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * The choice of the working sides of a block: a button standing just right of a panel, outside its frame, and the
 * window that opens to the right of the button when it is pressed, framed and backed as the panel is. The window shows
 * the six sides unfolded into a cross of squares. A click on a square moves the side to the next mode, a click with
 * the other button to the previous one; a square shows the mode by a sign: a cross for closed, an arrow in for input,
 * an arrow out for output, a diamond for both. What a click does goes to the server as a menu button. Whether the
 * window is open is only the screen's business and is not kept when it closes.
 */
final class SideModePanel<K extends Enum<K>> {

    private static final int BUTTON_SIZE = 18;
    private static final int BUTTON_TOP = 18;
    private static final int GAP = 4;
    private static final int SQUARE = 18;
    private static final int PITCH = 20;
    private static final int MARGIN = 8;
    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final int RIGHT_BUTTON = 1;
    private static final int CLOSED_FILL = 0xFF14151B;
    private static final int CLOSED_SIGN = 0xFF6B6F7A;
    private static final int INPUT_SIGN = 0xFF5FBF6B;
    private static final int OUTPUT_SIGN = 0xFFE59A4A;
    private static final int HEAD_ROWS = 4;
    private static final int SHAFT_ROWS = 4;
    private static final int SIGN_TOP = 4;
    private static final int SIGN_CENTRE = 9;
    private static final int ARROW_TOP = (SQUARE - HEAD_ROWS - SHAFT_ROWS) / 2;
    private static final int ICON_SQUARE = 4;
    private static final int ICON_PITCH = 5;
    /** The icon is 14 pixels square, the button 18: two pixels of margin on every side, the frame included. */
    private static final int ICON_OFFSET = 2;
    /** The squares of the icon of the button: the cross that a cube unfolds into, a square short of the window's. */
    private static final int[][] ICON = {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}};
    private final Map<K, int[]> net;
    private final Function<K, String> nameOf;
    private final PanelBounds button;
    private final PanelBounds window;
    private final int nextButtonId;
    private final int previousButtonId;
    private boolean open;

    /**
     * @param panel            the panel; the button stands just right of it
     * @param net              the cell of the cross for each side, {@code {column, row}}, see {@link SideLayouts}
     * @param nameOf           the word the name of a side in the language files ends in
     * @param nextButtonId     the menu button id for the first side, going to the next mode; the other sides follow
     * @param previousButtonId the same, going to the previous mode
     */
    SideModePanel(
            final PanelBounds panel, final Map<K, int[]> net, final Function<K, String> nameOf,
            final int nextButtonId, final int previousButtonId) {
        this.net = Map.copyOf(net);
        this.nameOf = nameOf;
        this.button = new PanelBounds(panel.left() + panel.width() + GAP, panel.top() + BUTTON_TOP, BUTTON_SIZE,
                BUTTON_SIZE);
        this.window = new PanelBounds(button.left() + BUTTON_SIZE + GAP, panel.top(),
                2 * MARGIN + COLUMNS * PITCH - (PITCH - SQUARE),
                PanelStyle.HEADER_HEIGHT + 2 * MARGIN + ROWS * PITCH - (PITCH - SQUARE));
        this.nextButtonId = nextButtonId;
        this.previousButtonId = previousButtonId;
    }

    void draw(
            final GuiGraphics graphics, final Font font, final PanelStyle style,
            final SideConfig<K> modes, final int mouseX, final int mouseY) {
        drawButton(graphics, style, mouseX, mouseY);
        if (!open) {
            return;
        }
        style.drawFrame(graphics, font, window, Component.translatable("gui.nexus.sides"));
        for (K side : net.keySet()) {
            final PanelBounds square = square(side);
            final SideMode mode = modes.mode(side);
            graphics.fill(square.left(), square.top(), square.left() + SQUARE, square.top() + SQUARE,
                    mode == SideMode.CLOSED ? CLOSED_FILL : style.buttonFill());
            graphics.renderOutline(square.left(), square.top(), SQUARE, SQUARE,
                    square.contains(mouseX, mouseY) ? PanelStyle.TEXT_LIGHT : style.border());
            drawSign(graphics, square, mode, style.accent());
        }
    }

    /**
     * @return the button, and the window while it is open, for a recipe viewer to keep clear of
     */
    List<Rect2i> areas() {
        return open ? List.of(button.toRect(), window.toRect()) : List.of(button.toRect());
    }

    /**
     * @return whether the click was on the button or on the window, and so was this panel's
     */
    boolean click(
            final Minecraft minecraft, final int containerId, final double x, final double y, final int mouseButton) {
        if (button.contains(x, y)) {
            open = !open;
            return true;
        }
        if (!open || !window.contains(x, y)) {
            return false;
        }
        final @Nullable K side = sideAt(x, y);
        if (side != null && minecraft.gameMode != null) {
            final int first = mouseButton == RIGHT_BUTTON ? previousButtonId : nextButtonId;
            minecraft.gameMode.handleInventoryButtonClick(containerId, first + side.ordinal());
        }
        return true;
    }

    void showTooltip(
            final GuiGraphics graphics, final Font font, final SideConfig<K> modes,
            final int mouseX, final int mouseY) {
        if (button.contains(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.nexus.sides"),
                    Component.translatable("gui.nexus.sides.hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        final @Nullable K side = open ? sideAt(mouseX, mouseY) : null;
        if (side != null) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.nexus.side.tooltip", Component.translatable(
                            "gui.nexus.side." + nameOf.apply(side)), modeName(modes.mode(side))),
                    Component.translatable("gui.nexus.side.hint").withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }
    }

    private void drawButton(
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        graphics.fill(button.left(), button.top(), button.left() + BUTTON_SIZE, button.top() + BUTTON_SIZE,
                style.buttonFill());
        graphics.renderOutline(button.left(), button.top(), BUTTON_SIZE, BUTTON_SIZE,
                open || button.contains(mouseX, mouseY) ? PanelStyle.TEXT_LIGHT : style.border());
        final int color = open ? style.accent() : PanelStyle.TEXT_DIM;
        for (int[] cell : ICON) {
            final int x = button.left() + ICON_OFFSET + cell[0] * ICON_PITCH;
            final int y = button.top() + ICON_OFFSET + cell[1] * ICON_PITCH;
            graphics.fill(x, y, x + ICON_SQUARE, y + ICON_SQUARE, color);
        }
    }

    private @Nullable K sideAt(final double x, final double y) {
        for (K side : net.keySet()) {
            if (square(side).contains(x, y)) {
                return side;
            }
        }
        return null;
    }

    private PanelBounds square(final K side) {
        final int[] cell = net.get(side);
        return new PanelBounds(window.left() + MARGIN + cell[0] * PITCH,
                window.top() + PanelStyle.HEADER_HEIGHT + MARGIN + cell[1] * PITCH, SQUARE, SQUARE);
    }

    private static Component modeName(final SideMode mode) {
        return Component.translatable("gui.nexus.side_mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    private static void drawSign(
            final GuiGraphics graphics, final PanelBounds square, final SideMode mode, final int accent) {
        switch (mode) {
            case CLOSED -> drawCross(graphics, square);
            case INPUT -> drawArrow(graphics, square, false, INPUT_SIGN);
            case OUTPUT -> drawArrow(graphics, square, true, OUTPUT_SIGN);
            case BOTH -> drawDiamond(graphics, square, accent);
        }
    }

    private static void drawCross(final GuiGraphics graphics, final PanelBounds square) {
        final int size = SQUARE - 2 * SIGN_TOP;
        for (int step = 0; step < size; step++) {
            graphics.fill(square.left() + SIGN_TOP + step, square.top() + SIGN_TOP + step,
                    square.left() + SIGN_TOP + step + 1, square.top() + SIGN_TOP + step + 1, CLOSED_SIGN);
            graphics.fill(square.left() + SIGN_TOP + size - 1 - step, square.top() + SIGN_TOP + step,
                    square.left() + SIGN_TOP + size - step, square.top() + SIGN_TOP + step + 1, CLOSED_SIGN);
        }
    }

    /**
     * An arrow that points out of the square, up, or into it, down.
     */
    private static void drawArrow(
            final GuiGraphics graphics, final PanelBounds square, final boolean up, final int color) {
        final int middle = square.left() + SIGN_CENTRE;
        final int total = HEAD_ROWS + SHAFT_ROWS;
        for (int row = 0; row < total; row++) {
            final int y = square.top() + ARROW_TOP + row;
            final int fromHead = up ? row : total - 1 - row;
            final int half = fromHead < HEAD_ROWS ? fromHead + 1 : 1;
            graphics.fill(middle - half, y, middle + half, y + 1, color);
        }
    }

    private static void drawDiamond(final GuiGraphics graphics, final PanelBounds square, final int color) {
        final int middle = square.left() + SIGN_CENTRE;
        for (int row = 0; row < 2 * HEAD_ROWS; row++) {
            final int half = row < HEAD_ROWS ? row + 1 : 2 * HEAD_ROWS - row;
            graphics.fill(middle - half, square.top() + ARROW_TOP + row, middle + half,
                    square.top() + ARROW_TOP + row + 1, color);
        }
    }
}
