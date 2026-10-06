package com.morphengine.nexus.storage;

/**
 * What the network may do with a block that holds its resources outside it, such as a chest.
 */
public enum ExternalAccess {

    /** The network lists what the block holds, takes it out and puts resources in. */
    READ_WRITE(true),

    /** The network lists what the block holds and takes it out, but never puts anything in. */
    READ_ONLY(false);

    private final boolean allowsInsert;

    ExternalAccess(final boolean allowsInsert) {
        this.allowsInsert = allowsInsert;
    }

    public boolean allowsInsert() {
        return allowsInsert;
    }
}
