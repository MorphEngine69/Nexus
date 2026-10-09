package com.morphengine.nexus.level;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A block entity whose energy buffer becomes part of the pool of the network it
 * is connected to.
 */
public interface EnergyContributor {

    EnergyBuffer energyBuffer();

    /**
     * @return where the buffer ranks in the pool, together with the priorities
     *         of the network's storages: higher is filled first and drained last
     */
    int energyPriority();

    /**
     * @return the same buffer as other blocks reach it, transaction-aware; the
     *         network goes through it when a transaction of another mod moves
     *         energy through the Nexus
     */
    IEnergyStorage energyHandler();

    /**
     * @return whether the block entity has left the level, removed or unloaded;
     *         its buffer must then no longer be counted
     */
    boolean isRemoved();
}
