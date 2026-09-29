package com.morphengine.nexus.api.resource;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A whitelist or blacklist of resources. A filter that lists nothing lets
 * every resource pass whatever its mode, so an unconfigured filter never
 * blocks anything. With {@link FilterMatchMode#EXACT} a resource must equal a
 * listed one exactly; a looser mode compares both sides {@linkplain
 * ResourceKey#normalized normalized} to it instead, so a listed item still
 * matches one that only differs in what that mode ignores.
 *
 * @param mode      whether the listed resources are the only ones allowed or the ones denied
 * @param matchMode how closely a resource must equal a listed one
 * @param resources the listed resources, normalized to {@code matchMode}; copied
 */
public record ResourceFilter(FilterMode mode, FilterMatchMode matchMode, Set<ResourceKey> resources) {

    /** Lets everything pass. */
    public static final ResourceFilter NONE = new ResourceFilter(FilterMode.ALLOW, Set.of());

    public ResourceFilter {
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(matchMode, "matchMode must not be null");
        Objects.requireNonNull(resources, "resources must not be null");
        final Set<ResourceKey> normalized = new HashSet<>(resources.size());
        for (ResourceKey resource : resources) {
            normalized.add(resource.normalized(matchMode));
        }
        resources = Set.copyOf(normalized);
    }

    /**
     * An exact filter: nothing is normalized before comparing.
     */
    public ResourceFilter(final FilterMode mode, final Set<ResourceKey> resources) {
        this(mode, FilterMatchMode.EXACT, resources);
    }

    public boolean allows(final ResourceKey resource) {
        if (resources.isEmpty()) {
            return true;
        }
        return resources.contains(resource.normalized(matchMode)) == (mode == FilterMode.ALLOW);
    }

    /**
     * @return whether the filter names {@code resource} as one of the few it is
     *         meant for: listed in a whitelist. A storage behind such a filter is
     *         where that resource should go first.
     */
    public boolean singlesOut(final ResourceKey resource) {
        return mode == FilterMode.ALLOW && resources.contains(resource.normalized(matchMode));
    }
}
