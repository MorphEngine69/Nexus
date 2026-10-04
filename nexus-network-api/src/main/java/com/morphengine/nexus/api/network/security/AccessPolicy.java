package com.morphengine.nexus.api.network.security;

import java.util.UUID;

/**
 * Who may do what with one network, by the players' ids. Answers for any
 * player, members and everyone else alike. Server side.
 */
@FunctionalInterface
public interface AccessPolicy {

    /** A network without any access rule: everyone may do everything. */
    AccessPolicy UNRESTRICTED = (player, permission) -> true;

    /**
     * Answers without allocating, as it is asked on every operation of every
     * device.
     *
     * @param player the id of the player, as the server knows them: from their
     *               account on a server that checks accounts, from their name otherwise
     * @return whether {@code player} holds {@code permission} in the network
     */
    boolean isAllowed(UUID player, Permission permission);
}
