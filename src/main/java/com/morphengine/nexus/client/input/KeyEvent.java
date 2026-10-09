package com.morphengine.nexus.client.input;

import net.minecraft.client.gui.screens.Screen;

/**
 * A key pressed in a screen.
 *
 * @param key       the key code
 * @param scanCode  the scan code of the key
 * @param modifiers the modifier keys held, as bits
 */
public record KeyEvent(int key, int scanCode, int modifiers) {

    public boolean hasControlDown() {
        return Screen.hasControlDown();
    }

    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
