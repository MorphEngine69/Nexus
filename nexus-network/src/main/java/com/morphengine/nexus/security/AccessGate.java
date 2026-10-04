package com.morphengine.nexus.security;

import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.storage.Actor;

import java.util.Objects;
import java.util.UUID;

/**
 * Decides whether an operation an {@link Actor} performs on a network may go
 * ahead. Asked on every insert and extract, so implementations must not
 * allocate. Server thread only.
 */
@FunctionalInterface
public interface AccessGate {

    /** Lets everything through. */
    AccessGate UNRESTRICTED = (actor, permission) -> true;

    boolean permits(Actor actor, Permission permission);

    /**
     * @return a gate that asks {@code policy} about the {@linkplain Actor#player
     *         player} an actor acts for; an actor that acts for no player is the
     *         network doing its own work and passes
     */
    static AccessGate of(final AccessPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        return (actor, permission) -> {
            final UUID player = actor.player();
            return player == null || policy.isAllowed(player, permission);
        };
    }
}
