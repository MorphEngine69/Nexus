package com.morphengine.nexus.api.resource;

import java.util.Objects;
import java.util.Set;

/**
 * A whitelist or blacklist of resources, matched exactly. A filter that lists
 * nothing lets every resource pass whatever its mode, so an unconfigured filter
 * never blocks anything.
 *
 * @param mode      whether the listed resources are the only ones allowed or the ones denied
 * @param resources the listed resources; copied
 */
public record ResourceFilter(FilterMode mode, Set<ResourceKey> resources) {

    /** Lets everything pass. */
    public static final ResourceFilter NONE = new ResourceFilter(FilterMode.ALLOW, Set.of());

    public ResourceFilter {
        Objects.requireNonNull(mode, "mode must not be null");
        resources = Set.copyOf(Objects.requireNonNull(resources, "resources must not be null"));
    }

    public boolean allows(final ResourceKey resource) {
        if (resources.isEmpty()) {
            return true;
        }
        return resources.contains(resource) == (mode == FilterMode.ALLOW);
    }

    /**
     * @return whether the filter names {@code resource} as one of the few it is
     *         meant for: listed in a whitelist. A storage behind such a filter is
     *         where that resource should go first.
     */
    public boolean singlesOut(final ResourceKey resource) {
        return mode == FilterMode.ALLOW && resources.contains(resource);
    }
}
