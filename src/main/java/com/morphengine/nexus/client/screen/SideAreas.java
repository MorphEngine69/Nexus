package com.morphengine.nexus.client.screen;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * A screen with buttons or windows beside its frame. A recipe viewer such as JEI or REI is told where they are, so
 * that its overlay does not cover them: the player could not click them otherwise.
 */
public interface SideAreas {

    /**
     * @return the rectangles outside the frame that the screen uses now, in screen coordinates; the answer follows
     *         what is open, as a window that shows only after a click
     */
    List<Rect2i> extraAreas();
}
