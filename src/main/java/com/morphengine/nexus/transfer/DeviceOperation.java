package com.morphengine.nexus.transfer;

/**
 * What one operation of an attached device does where it works. An operation
 * may remember where it left off, such as the next entry of a round robin,
 * between runs. Server thread only.
 */
@FunctionalInterface
public interface DeviceOperation {

    /**
     * @return units moved; zero when nothing could move
     */
    long run(Workplace workplace);
}
