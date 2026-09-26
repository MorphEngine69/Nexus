package com.morphengine.nexus.api.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;

/**
 * A storage resources can be taken out of.
 */
public interface ExtractableStorage {

    /**
     * Extracts up to {@code amount} of the resource. A partial extract is normal,
     * not an error.
     *
     * @param amount units requested, must be positive
     * @param actor  who the operation is performed for
     * @return units removed, from zero to {@code amount}; under
     *         {@link Action#SIMULATE} the units that would be removed, with
     *         nothing changed
     */
    long extract(ResourceKey resource, long amount, Action action, Actor actor);
}
