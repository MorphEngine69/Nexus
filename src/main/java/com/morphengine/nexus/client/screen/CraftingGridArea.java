package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.terminal.TerminalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The crafting grid of a Crafting Terminal around its slots: an arrow to the
 * result and a button that returns the grid to the network.
 */
final class CraftingGridArea implements TerminalWorkArea {

    private static final int CLEAR_SIZE = 10;
    private static final int CLEAR_GAP = 2;
    private static final int ARROW_LENGTH = 22;
    private static final int ARROW_GAP = 6;
    private static final int ARROW_HEAD = 4;

    private final int containerId;
    private final PanelBounds clearButton;
    private final int arrowLeft;
    private final int arrowTop;

    CraftingGridArea(final int containerId, final int panelLeft, final int panelTop, final TerminalLayout layout) {
        this.containerId = containerId;
        this.clearButton = clearButtonOf(panelLeft, panelTop, layout);
        this.arrowLeft = panelLeft + layout.craftingRight() + ARROW_GAP;
        this.arrowTop = panelTop + layout.resultTop() + TerminalLayout.SLOT / 2 - 2;
    }

    /**
     * @return the button that empties the grid, right of its first row; the
     *         Blueprint encoder puts its own there too
     */
    static PanelBounds clearButtonOf(final int panelLeft, final int panelTop, final TerminalLayout layout) {
        return new PanelBounds(panelLeft + layout.craftingRight() + CLEAR_GAP, panelTop + layout.craftingTop() - 1,
                CLEAR_SIZE, CLEAR_SIZE);
    }

    @Override
    public void draw(final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        drawArrow(graphics, arrowLeft, arrowTop, style.border());
        style.drawCrossButton(graphics, clearButton, clearButton.contains(mouseX, mouseY));
    }

    /**
     * A two pixel shaft and a head that narrows to the tip, pointing right.
     */
    static void drawArrow(final GuiGraphics graphics, final int left, final int top, final int color) {
        final int headLeft = left + ARROW_LENGTH - ARROW_HEAD;
        graphics.fill(left, top, headLeft, top + 2, color);
        for (int step = 0; step < ARROW_HEAD; step++) {
            final int half = ARROW_HEAD - 1 - step;
            graphics.fill(headLeft + step, top - half, headLeft + step + 1, top + 2 + half, color);
        }
    }

    @Override
    public List<Component> tooltip(final int mouseX, final int mouseY) {
        return clearButton.contains(mouseX, mouseY)
                ? List.of(Component.translatable("gui.nexus.terminal.clear_grid")) : List.of();
    }

    @Override
    public boolean click(final MouseButtonEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!clearButton.contains(event.x(), event.y()) || minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(containerId, CraftingTerminalMenu.BUTTON_CLEAR_GRID);
        return true;
    }
}
