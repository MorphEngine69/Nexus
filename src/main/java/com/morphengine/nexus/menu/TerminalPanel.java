package com.morphengine.nexus.menu;

import com.morphengine.nexus.terminal.TerminalLayout;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A menu of a terminal of any kind, on a block or in a hand. Its title cannot
 * be renamed.
 */
public interface TerminalPanel extends PanelMenu, NetworkBadgeView {

    TerminalMenuState terminal();

    /**
     * Moves the slots to where {@code layout} puts them. The screen calls it
     * whenever it picks a layout; the server never needs slot positions.
     */
    void layOut(TerminalLayout layout);

    @Override
    default Component defaultTitle() {
        return terminal().binding().defaultTitle();
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
