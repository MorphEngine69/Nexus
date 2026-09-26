package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.Objects;

/**
 * A resource a Pusher delivers, and how much of it the storage beside the
 * device should end up holding.
 *
 * @param keep units of the resource the destination is kept stocked with,
 *             positive; {@link #UNLIMITED} delivers as long as it takes more
 */
public record PushEntry(ResourceKey resource, long keep) {

    public static final long UNLIMITED = Long.MAX_VALUE;

    public PushEntry {
        Objects.requireNonNull(resource, "resource must not be null");
        if (keep <= 0) {
            throw new IllegalArgumentException("amount of " + resource + " to keep must be positive: " + keep);
        }
    }

    public static PushEntry unlimited(final ResourceKey resource) {
        return new PushEntry(resource, UNLIMITED);
    }
}
