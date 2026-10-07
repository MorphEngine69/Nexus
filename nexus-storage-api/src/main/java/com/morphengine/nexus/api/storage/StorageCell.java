package com.morphengine.nexus.api.storage;

import com.morphengine.nexus.api.resource.ResourceType;

/**
 * A storage with a fixed capacity in the byte model of {@link CellSpec} that
 * holds resources of one type only.
 */
public interface StorageCell extends Storage {

    /**
     * @return the only kind of resource the cell accepts
     */
    ResourceType type();

    CellUsage usage();
}
