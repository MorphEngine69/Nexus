package com.morphengine.nexus.api.storage;

import com.morphengine.nexus.api.resource.ResourceKey;

/**
 * Told when the amount of a resource in an observed storage changes. Called on
 * the server thread, right after the change, once per resource per change.
 */
@FunctionalInterface
public interface StorageListener {

    /**
     * @param amount units of {@code resource} the storage holds now; zero when it is gone
     */
    void onAmountChanged(ResourceKey resource, long amount);
}
