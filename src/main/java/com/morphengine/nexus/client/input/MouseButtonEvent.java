package com.morphengine.nexus.client.input;

import net.minecraft.client.gui.screens.Screen;

/**
 * A press, drag or release of a mouse button in a screen.
 *
 * @param x      where the pointer is, in the coordinates of the screen
 * @param y      where the pointer is, in the coordinates of the screen
 * @param button the button, 0 for the left one
 */
public record MouseButtonEvent(double x, double y, int button) {

    public boolean hasControlDown() {
        return Screen.hasControlDown();
    }

    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
