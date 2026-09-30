package com.morphengine.nexus.client.screen;

import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

/**
 * What a mouse click means in a panel.
 */
final class MouseButtons {

    private MouseButtons() {
    }

    /**
     * @return whether the right button was pressed: half a stack, one item, or a step back
     */
    static boolean isSecondary(final MouseButtonEvent event) {
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }
}
