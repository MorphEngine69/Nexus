package com.morphengine.nexus.api.network.security;

/**
 * How one {@link Permission} of a member stands apart from their {@link Role}.
 */
public enum PermissionState {

    /** As the role says. */
    INHERIT,

    /** Held, whatever the role says. */
    ALLOW,

    /** Not held, whatever the role says. */
    DENY
}
