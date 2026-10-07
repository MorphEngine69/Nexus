package com.morphengine.nexus.api.resource;

import java.util.Objects;

/**
 * A resource and how much of it there is.
 *
 * @param amount units of the resource, always positive; what a unit is depends
 *               on the resource type, such as one item or one millibucket
 */
public record ResourceAmount(ResourceKey resource, long amount) {

    public ResourceAmount {
        Objects.requireNonNull(resource, "resource must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount of " + resource + " must be positive: " + amount);
        }
    }
}
