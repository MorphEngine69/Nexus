package com.morphengine.nexus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;

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
        return event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }
}
