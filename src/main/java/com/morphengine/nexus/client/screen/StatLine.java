package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines of figures on a panel. A line wraps when its words, in a language
 * with long ones, do not fit the width left for it, and shows its exact figures
 * in a tooltip when they are written short. One instance per screen: it
 * remembers where the lines of the last frame stood.
 */
final class StatLine {

    private static final int LINE_GAP = 2;

    private final List<Shown> shown = new ArrayList<>();

    /**
     * Forgets the lines of the last frame; called before drawing the next.
     */
    void begin() {
        shown.clear();
    }

    /**
     * @param area where the line starts and how wide it may be before it wraps; its height is ignored
     * @return the height the line took, to start the next one below it
     */
    int draw(final GuiGraphicsExtractor graphics, final Font font, final Stat stat, final PanelBounds area,
             final int color) {
        graphics.textWithWordWrap(font, stat.text(), area.left(), area.top(), area.width(), color);
        final int height = font.split(stat.text(), area.width()).size() * font.lineHeight;
        if (stat.exact() != null) {
            shown.add(new Shown(new PanelBounds(area.left(), area.top(), area.width(), height), stat.exact()));
        }
        return height + LINE_GAP;
    }

    /**
     * Shows the exact figures of the line under the mouse, if it was written short.
     */
    void showTooltip(final GuiGraphicsExtractor graphics, final Font font, final int mouseX, final int mouseY) {
        for (Shown line : shown) {
            if (line.bounds().contains(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(line.exact()), mouseX, mouseY);
                return;
            }
        }
    }

    /**
     * A line to draw.
     *
     * @param text  the line as the panel shows it
     * @param exact the exact figures for the tooltip; {@code null} when the line already shows them
     */
    record Stat(Component text, @Nullable Component exact) {

        static Stat plain(final Component text) {
            return new Stat(text, null);
        }
    }

    private record Shown(PanelBounds bounds, Component exact) {
    }
}
