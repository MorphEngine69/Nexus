package com.morphengine.nexus.block.entity;

import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Tells a held right button apart from a new click. The client repeats the
 * interaction every few ticks while the button is down, so a click that places
 * a device would open its menu a moment later, and Escape pressed with the
 * button down would reopen the menu at once. Clicks shortly after either are
 * ignored.
 */
final class ClickGuard {

    private static final int AFTER_CLOSE_TICKS = 2;
    /** Longer than the client's repeat delay of four ticks. */
    private static final int AFTER_PLACE_TICKS = 10;

    /** Far enough in the past that subtracting it from a game time does not overflow. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    private long closedAt = NEVER;
    private long placedAt = NEVER;

    void markClosed(final @Nullable Level level) {
        if (level != null) {
            closedAt = level.getGameTime();
        }
    }

    void markPlaced(final @Nullable Level level) {
        if (level != null) {
            placedAt = level.getGameTime();
        }
    }

    boolean ignoresClick(final @Nullable Level level) {
        if (level == null) {
            return false;
        }
        final long now = level.getGameTime();
        return now - closedAt < AFTER_CLOSE_TICKS || now - placedAt < AFTER_PLACE_TICKS;
    }
}
