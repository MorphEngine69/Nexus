package com.morphengine.nexus.level;

import org.jspecify.annotations.Nullable;

/**
 * The Nexus a {@link NetworkMember} was last told it belongs to. A member keeps
 * one and forwards its join and leave calls here. Server thread only.
 */
public final class NetworkLink {

    private @Nullable NetworkController controller;

    public void join(final NetworkController joined) {
        controller = joined;
    }

    /**
     * Only takes effect if {@code left} is the controller last joined.
     */
    public void leave(final NetworkController left) {
        if (controller == left) {
            controller = null;
        }
    }

    /**
     * @return the controller of the network, or {@code null} when there is none
     *         or it has left the level
     */
    public @Nullable NetworkController controller() {
        final NetworkController current = controller;
        return current == null || current.isRemoved() ? null : current;
    }
}
