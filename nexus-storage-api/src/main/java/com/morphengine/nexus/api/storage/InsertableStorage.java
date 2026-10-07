package com.morphengine.nexus.api.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;

/**
 * A storage resources can be put into.
 */
public interface InsertableStorage {

    /**
     * Inserts as much of the resource as fits. A partial insert is normal, not an
     * error: the caller keeps whatever was not accepted.
     *
     * @param amount units offered, must be positive
     * @param actor  who the operation is performed for
     * @return units accepted, from zero to {@code amount}; under
     *         {@link Action#SIMULATE} the units that would be accepted, with
     *         nothing changed
     */
    long insert(ResourceKey resource, long amount, Action action, Actor actor);
}
