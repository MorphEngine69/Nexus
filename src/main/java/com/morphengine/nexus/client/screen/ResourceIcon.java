package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A 16x16 icon of one resource, ready to draw.
 */
@FunctionalInterface
interface ResourceIcon {

    /** Draws nothing: the icon of a resource kind without a renderer. */
    ResourceIcon NONE = (graphics, x, y) -> { };

    /**
     * Draws the icon with its top left corner at {@code (x, y)}.
     */
    void draw(GuiGraphics graphics, int x, int y);
}
