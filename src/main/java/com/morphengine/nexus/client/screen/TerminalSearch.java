package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The search box of a terminal with a reset cross at its right end. It keeps
 * the keyboard only while the player is searching: erasing the text to nothing,
 * the cross or a click elsewhere hands the keys back to the panel, so the
 * inventory key closes it again. The text stays while the player takes what
 * was found. Client thread only.
 */
final class TerminalSearch {

    private static final int INSET = 2;
    private static final List<Component> SEARCH_HELP = List.of(
            Component.translatable("gui.nexus.terminal.search.help.title"),
            Component.translatable("gui.nexus.terminal.search.help.mod").withColor(PanelStyle.TEXT_DIM),
            Component.translatable("gui.nexus.terminal.search.help.tag").withColor(PanelStyle.TEXT_DIM),
            Component.translatable("gui.nexus.terminal.search.help.tooltip").withColor(PanelStyle.TEXT_DIM),
            Component.translatable("gui.nexus.terminal.search.help.not").withColor(PanelStyle.TEXT_DIM),
            Component.translatable("gui.nexus.terminal.search.help.or").withColor(PanelStyle.TEXT_DIM),
            Component.translatable("gui.nexus.terminal.search.help.group").withColor(PanelStyle.TEXT_DIM));

    private final PanelBounds frame;
    private final PanelBounds reset;
    private final EditBox box;

    /**
     * @param frame the box's area, one pixel of border included
     */
    TerminalSearch(final Font font, final PanelBounds frame, final String text) {
        this.frame = frame;
        final int resetSize = frame.height() - 2;
        this.reset = new PanelBounds(frame.left() + frame.width() - 1 - resetSize, frame.top() + 1,
                resetSize, resetSize);
        final Component hint = Component.translatable("gui.nexus.terminal.search");
        this.box = new EditBox(font, frame.left() + INSET, frame.top() + INSET,
                frame.width() - INSET * 2 - resetSize, frame.height() - INSET, hint);
        box.setBordered(false);
        box.setTextColor(PanelStyle.TEXT_LIGHT);
        box.setHint(hint);
        box.setValue(text);
    }

    EditBox widget() {
        return box;
    }

    String text() {
        return box.getValue();
    }

    /**
     * @return what to say about the part of the box under the cursor: how to clear it, or what it understands; nothing
     *         when the cursor is off the box
     */
    List<Component> tooltip(final double x, final double y) {
        if (isOverReset(x, y)) {
            return List.of(Component.translatable("gui.nexus.terminal.clear_search"));
        }
        return frame.contains(x, y) ? SEARCH_HELP : List.of();
    }

    boolean isOverReset(final double x, final double y) {
        return !box.getValue().isEmpty() && reset.contains(x, y);
    }

    void draw(final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        graphics.fill(frame.left(), frame.top(), frame.left() + frame.width(), frame.top() + frame.height(),
                style.track());
        graphics.outline(frame.left(), frame.top(), frame.width(), frame.height(), style.border());
        if (box.getValue().isEmpty()) {
            return;
        }
        PanelStyle.drawCross(graphics, reset,
                reset.contains(mouseX, mouseY) ? PanelStyle.TEXT_LIGHT : PanelStyle.TEXT_DIM);
    }

    /**
     * A click on the cross or a right click on the box empties it; any click
     * off the box takes the keyboard away from it.
     *
     * @return whether the click was used up
     */
    boolean click(final double x, final double y, final boolean secondary) {
        if (isOverReset(x, y) || secondary && frame.contains(x, y)) {
            clear();
            return true;
        }
        if (!frame.contains(x, y)) {
            box.setFocused(false);
        }
        return false;
    }

    /**
     * While the box has focus it takes every key but Escape, so typing the
     * inventory key does not close the panel. Erasing the last character
     * gives the keys back.
     *
     * @return whether the key was used up
     */
    boolean keyPressed(final KeyEvent event) {
        if (!box.isFocused() || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        final boolean hadText = !box.getValue().isEmpty();
        box.keyPressed(event);
        if (hadText && box.getValue().isEmpty()) {
            box.setFocused(false);
        }
        return true;
    }

    void clear() {
        box.setValue("");
        box.setFocused(false);
    }
}
