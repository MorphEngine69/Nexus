package com.morphengine.nexus.transport;

/**
 * What one operation of a device that moves resources does along its route.
 */
@FunctionalInterface
public interface TransferTask {

    /**
     * @return units moved; zero when nothing could move
     */
    long runOnce(StorageRoute route);
}
