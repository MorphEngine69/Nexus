package com.morphengine.nexus.api.storage;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

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

    /**
     * @return the player whose access rights the operation is subject to: the
     *         player at a terminal, or the player a device works for; {@code null}
     *         for an operation the network performs on its own, such as a
     *         crafting task moving what it holds, which no access rule restricts
     */
    default @Nullable UUID player() {
        return null;
    }
}
