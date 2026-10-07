package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.Objects;

/**
 * A resource a device moves, and how much of it stays where the device keeps
 * stock: a Pusher fills the storage beside it up to that much, a Puller leaves
 * at least that much in it.
 *
 * @param keep units of the resource kept, positive; {@link #UNLIMITED} for a
 *             Pusher that delivers as long as the storage takes more
 */
public record StockEntry(ResourceKey resource, long keep) implements PushEntry {

    public static final long UNLIMITED = Long.MAX_VALUE;

    public StockEntry {
        Objects.requireNonNull(resource, "resource must not be null");
        if (keep <= 0) {
            throw new IllegalArgumentException("amount of " + resource + " to keep must be positive: " + keep);
        }
    }

    public static StockEntry unlimited(final ResourceKey resource) {
        return new StockEntry(resource, UNLIMITED);
    }
}
