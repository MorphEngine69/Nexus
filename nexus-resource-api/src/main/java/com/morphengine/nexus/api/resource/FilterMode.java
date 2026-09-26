package com.morphengine.nexus.api.resource;

/**
 * How a {@link ResourceFilter} treats the resources listed in it.
 */
public enum FilterMode {

    /** Whitelist: only listed resources pass. */
    ALLOW,

    /** Blacklist: everything except the listed resources passes. */
    DENY;

    public FilterMode toggled() {
        return this == ALLOW ? DENY : ALLOW;
    }
}
