package com.morphengine.nexus.access;

import com.morphengine.nexus.api.network.security.AccessPolicy;

import java.util.UUID;

/**
 * A block entity whose block falls under the access rules of a network: who
 * may open it, change it, take it down. Server side only.
 */
public interface Secured {

    /**
     * Cheap enough to ask on every tick a panel of the block is open.
     *
     * @return the rules of the network the block belongs to now; for a block
     *         outside every network, the rules of the network it was last in,
     *         or failing that, those of whoever placed it
     */
    AccessPolicy accessPolicy();

    /**
     * @return whether {@code player} placed the block, so that they may always
     *         take it down again, whatever the network says
     */
    default boolean isOwnedBy(final UUID player) {
        return false;
    }
}
