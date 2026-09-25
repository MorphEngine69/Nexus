package com.morphengine.nexus.level;

import com.morphengine.nexus.api.energy.EnergyBuffer;

/**
 * A block entity whose energy buffer becomes part of the pool of the network it
 * is connected to.
 */
public interface EnergyContributor {

    EnergyBuffer energyBuffer();

    /**
     * @return whether the block entity has left the level, removed or unloaded;
     *         its buffer must then no longer be counted
     */
    boolean isRemoved();
}
