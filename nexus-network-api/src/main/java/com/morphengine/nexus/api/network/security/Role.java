package com.morphengine.nexus.api.network.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The standing of a player in a network: which {@link Permission}s they hold
 * before any adjustment, and whom they may manage. Declared from the highest
 * standing down.
 */
public enum Role {

    /** The one player the network belongs to: everything, always, and nobody can change that but them. */
    OWNER(Permission.values()),

    /** Everything, managing the access of users, guests and blocked players included. */
    ADMIN(Permission.values()),

    /** Works with the network as a member of the team: everything but managing access. */
    USER(Permission.OPEN, Permission.INSERT, Permission.EXTRACT, Permission.AUTOCRAFTING, Permission.CONFIGURE,
            Permission.BUILD),

    /** Looks without touching: opens panels and terminals and sees what the network holds. */
    GUEST(Permission.OPEN),

    /** Nothing at all, whatever their adjustments say. */
    BLOCKED;

    private final Set<Permission> permissions;

    Role(final Permission... granted) {
        final Set<Permission> set = EnumSet.noneOf(Permission.class);
        Collections.addAll(set, granted);
        this.permissions = Collections.unmodifiableSet(set);
    }

    /**
     * @return the permissions the role holds before any adjustment; unmodifiable
     */
    public Set<Permission> permissions() {
        return permissions;
    }

    public boolean grants(final Permission permission) {
        return permissions.contains(permission);
    }

    /**
     * @return whether a network may give this role to everyone who is not a member
     */
    public boolean isDefaultable() {
        return this == USER || this == GUEST || this == BLOCKED;
    }

    /**
     * @return whether permissions of a member with this role may be adjusted one
     *         by one; the owner always holds everything and a blocked player nothing
     */
    public boolean isAdjustable() {
        return this == ADMIN || this == USER || this == GUEST;
    }
}
