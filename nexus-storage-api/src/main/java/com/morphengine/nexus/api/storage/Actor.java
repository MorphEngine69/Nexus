package com.morphengine.nexus.api.storage;

/**
 * Who a storage operation is performed for: a player at a terminal, a device,
 * an automation job. Every insert and extract carries one, so that access rules
 * can be checked and changes attributed without changing any signature.
 */
@FunctionalInterface
public interface Actor {

    /** An operation nobody in particular asked for, such as moving contents between storages. */
    Actor NOBODY = () -> "";

    /**
     * @return a name to show to players, such as a player name; empty for {@link #NOBODY}
     */
    String name();
}
