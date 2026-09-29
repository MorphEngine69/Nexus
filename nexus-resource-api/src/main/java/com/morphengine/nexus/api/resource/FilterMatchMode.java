package com.morphengine.nexus.api.resource;

/**
 * How closely a {@link ResourceFilter} compares a resource against what it
 * lists. What each looser mode ignores is up to the {@link ResourceKey}
 * implementation: a kind with nothing to ignore treats every mode the same.
 */
public enum FilterMatchMode {

    /** The resource must equal a listed one exactly. */
    EXACT,

    /** A listed resource matches regardless of wear, such as an item's damage. */
    IGNORE_DURABILITY,

    /** A listed resource matches regardless of any of its components. */
    IGNORE_COMPONENTS
}
