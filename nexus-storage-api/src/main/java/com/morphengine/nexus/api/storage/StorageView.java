package com.morphengine.nexus.api.storage;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.List;

/**
 * Read access to what a storage holds.
 */
public interface StorageView {

    /**
     * @return units of {@code resource} held; zero when there is none
     */
    long amountOf(ResourceKey resource);

    /**
     * @return every resource held, each once; an unmodifiable snapshot that does
     *         not follow later changes
     */
    List<ResourceAmount> contents();
}
