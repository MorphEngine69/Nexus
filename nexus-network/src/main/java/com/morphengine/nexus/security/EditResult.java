package com.morphengine.nexus.security;

/**
 * What became of a {@link SecurityEdit}.
 */
public enum EditResult {

    /** Made. */
    APPLIED,

    /** Nothing to do: things already stood as asked. */
    UNCHANGED,

    /** The editor may not make this change. */
    DENIED,

    /**
     * The change makes no sense as asked: a player who is not a member, a role
     * or permission that cannot be given that way, a network already claimed.
     */
    INVALID,

    /** The network has as many members as it can have. */
    FULL
}
