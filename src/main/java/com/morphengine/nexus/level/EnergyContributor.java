package com.morphengine.nexus.level;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * A block entity whose energy buffer becomes part of the pool of the network it
 * is connected to.
 */
public interface EnergyContributor {

    EnergyBuffer energyBuffer();

    /**
     * @return the same buffer as other blocks reach it, transaction-aware; the
     *         network goes through it when a transaction of another mod moves
     *         energy through the Nexus
     */
    EnergyHandler energyHandler();

    /**
     * @return whether the block entity has left the level, removed or unloaded;
     *         its buffer must then no longer be counted
     */
    boolean isRemoved();
}
