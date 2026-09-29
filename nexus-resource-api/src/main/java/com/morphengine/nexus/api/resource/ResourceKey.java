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

    /**
     * @return this resource under {@code mode}: the same key for {@link
     *         FilterMatchMode#EXACT} and for a kind with nothing a looser mode
     *         could ignore; two resources a looser mode does not tell apart
     *         normalize to equal keys, so a {@link ResourceFilter} can compare
     *         them as one
     */
    default ResourceKey normalized(final FilterMatchMode mode) {
        return this;
    }
}
