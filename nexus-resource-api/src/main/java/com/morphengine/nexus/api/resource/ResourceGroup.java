package com.morphengine.nexus.api.resource;

/**
 * A family of resources that a {@link ResourceFilter} lists as one entry, such
 * as every item in a tag. Membership is decided by the group, not by the
 * filter, so it follows whatever the group is backed by, including resources
 * added after the filter was set.
 *
 * <p>Implementations must be immutable and implement {@code equals} and
 * {@code hashCode}: two groups with the same members are equal.
 */
public interface ResourceGroup {

    /**
     * Called by filters for every resource they check, so it must be cheap and
     * must not allocate.
     *
     * @return whether {@code resource} belongs to this group
     */
    boolean contains(ResourceKey resource);
}
