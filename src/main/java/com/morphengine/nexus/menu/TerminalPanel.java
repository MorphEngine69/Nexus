package com.morphengine.nexus.menu;

import com.morphengine.nexus.terminal.TerminalLayout;
import org.jspecify.annotations.Nullable;

/**
 * A menu of a Terminal or Crafting Terminal.
 */
public interface TerminalPanel extends DevicePanel, NetworkBadgeView {

    TerminalMenuState terminal();

    /**
     * Moves the slots to where {@code layout} puts them. The screen calls it
     * whenever it picks a layout; the server never needs slot positions.
     */
    void layOut(TerminalLayout layout);

    @Override
    default DeviceBinding<?> binding() {
        return terminal().binding();
    }

    @Override
    default @Nullable NetworkBadge badge() {
        return terminal().badge();
    }

    @Override
    default void acceptBadge(final @Nullable NetworkBadge received) {
        terminal().acceptBadge(received);
    }
}
