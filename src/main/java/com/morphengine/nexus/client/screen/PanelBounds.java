package com.morphengine.nexus.client.screen;

/**
 * A rectangle on screen, in GUI pixels.
 */
record PanelBounds(int left, int top, int width, int height) {

    boolean contains(final double x, final double y) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
