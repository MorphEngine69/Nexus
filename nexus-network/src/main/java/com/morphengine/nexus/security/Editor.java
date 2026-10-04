package com.morphengine.nexus.security;

import java.util.Objects;
import java.util.UUID;

/**
 * Who makes a change to the access of a network, and with what standing.
 *
 * @param id the editor's player id, the one their role in the network is looked up by
 */
public record Editor(UUID id, Standing standing) {

    public Editor {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(standing, "standing must not be null");
    }

    public static Editor player(final UUID id) {
        return new Editor(id, Standing.PLAYER);
    }

    public static Editor operator(final UUID id) {
        return new Editor(id, Standing.OPERATOR);
    }

    /**
     * Where the editor stands apart from their role in the network.
     */
    public enum Standing {

        /** Only their role in the network counts. */
        PLAYER,

        /**
         * An operator of the server, or the player hosting a world opened to
         * the local network: above every network, its owner included.
         */
        OPERATOR
    }
}
