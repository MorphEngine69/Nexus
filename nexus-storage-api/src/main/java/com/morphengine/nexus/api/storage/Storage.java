package com.morphengine.nexus.api.storage;

import com.morphengine.nexus.api.resource.ResourceKey;

/**
 * A place that holds resources and lets them be inserted and extracted.
 * Implementations are not thread-safe and are used from the server thread only.
 */
public interface Storage extends StorageView, InsertableStorage, ExtractableStorage {

    /**
     * Whether this storage is set aside for {@code resource}, such as a cell
     * whose whitelist lists it. When several storages of equal priority can take
     * a resource, those set aside for it, or already holding it, are filled first.
     *
     * @return {@code false} unless the storage singles the resource out
     */
    default boolean isReservedFor(final ResourceKey resource) {
        return false;
    }
}
