package com.morphengine.nexus.level;

import com.morphengine.nexus.energy.DeviceEnergyMeter;

/**
 * A device that keeps count of the energy it draws, supplies and pays in tolls, so that the statistics of its network
 * and an Analyser can tell what it does. Server thread only.
 */
public interface MeteredDevice {

    DeviceEnergyMeter energyMeter();

    /**
     * @param gameTime the current game time in ticks
     * @return the device as a row of the statistics, with the figures of the last measured period
     */
    DeviceEnergyRow energyRow(long gameTime);
}
