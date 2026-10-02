package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;

import java.util.Objects;

/**
 * Checks of the arguments every insert and extract takes, for storages of the
 * network and of the world around it alike.
 */
public final class StorageArguments {

    private StorageArguments() {
    }

    /**
     * @throws IllegalArgumentException if {@code amount} is not positive
     */
    public static void check(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount of " + resource + " must be positive: " + amount);
        }
    }
}
