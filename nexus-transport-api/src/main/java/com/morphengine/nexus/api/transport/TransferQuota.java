package com.morphengine.nexus.api.transport;

import com.morphengine.nexus.api.resource.ResourceKey;

/**
 * How much of a resource one operation of a device moves at most.
 */
@FunctionalInterface
public interface TransferQuota {

    /**
     * @return units of {@code resource} one operation moves at most, positive;
     *         what a unit is depends on the resource type, such as one item or
     *         one millibucket
     */
    long unitsPerOperation(ResourceKey resource);
}
