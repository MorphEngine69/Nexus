package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.PriorityButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A device's priority written on the left of a panel and the buttons that
 * change it on the right, as {@link PriorityButtons} lays them out. A click on
 * a button goes to the server as a menu button.
 */
final class PriorityRow {

    private static final int BUTTON_WIDTH = 22;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;

    private final List<PanelBounds> buttons = new ArrayList<>(PriorityButtons.count());
    private final int textLeft;
    private final int top;
    private final int firstButtonId;

    /**
     * @param firstButtonId the menu button id of the leftmost button; the others follow it
     */
    PriorityRow(final PanelBounds panel, final int top, final int firstButtonId) {
        this.textLeft = panel.left() + PanelStyle.PADDING;
        this.top = top;
        this.firstButtonId = firstButtonId;
        final int firstLeft = panel.left() + panel.width() - PanelStyle.PADDING
                - PriorityButtons.count() * (BUTTON_WIDTH + BUTTON_GAP) + BUTTON_GAP;
        for (int i = 0; i < PriorityButtons.count(); i++) {
            buttons.add(new PanelBounds(firstLeft + i * (BUTTON_WIDTH + BUTTON_GAP), top, BUTTON_WIDTH,
                    BUTTON_HEIGHT));
        }
    }

    void draw(final GuiGraphicsExtractor graphics, final Font font, final PanelStyle style, final int priority) {
        graphics.text(font, Component.translatable("gui.nexus.vault.priority", priority), textLeft,
                top + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1, PanelStyle.TEXT_LIGHT, false);
        for (int i = 0; i < buttons.size(); i++) {
            style.drawButton(graphics, font, buttons.get(i), Component.literal(PriorityButtons.labelOf(i)));
        }
    }

    /**
     * @return whether a button was under the cursor and its click was sent
     */
    boolean click(final Minecraft minecraft, final int containerId, final double x, final double y) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).contains(x, y) && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(containerId, firstButtonId + i);
                return true;
            }
        }
        return false;
    }
}
