package com.morphengine.nexus.api.resource;

/**
 * Identity of one resource, without an amount: an item with its components, a
 * fluid, and so on. Two keys are the same resource exactly when they are
 * {@linkplain Object#equals equal}, so implementations must be immutable and
 * implement {@code equals} and {@code hashCode} over everything that tells
 * resources apart.
 */
public interface ResourceKey {

    /**
     * @return the kind of this resource; the same instance for every key of that kind
     */
    ResourceType type();
}
