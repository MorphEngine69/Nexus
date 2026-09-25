package com.morphengine.nexus.api.network;

/**
 * Figures shown in the Nexus interface. Energy values are in RF, rates in RF per tick.
 *
 * @param devices        devices connected to the Nexus; cables and the Nexus itself are not counted
 * @param energyStored   RF held by all energy buffers of the network
 * @param energyCapacity RF all energy buffers of the network can hold
 * @param energyInput    average RF/t entering the network's buffers
 * @param energyOutput   average RF/t leaving the network's buffers
 */
public record NetworkStatistics(
        int devices, long energyStored, long energyCapacity, long energyInput, long energyOutput) {

    public static final NetworkStatistics EMPTY = new NetworkStatistics(0, 0, 0, 0, 0);

    public NetworkStatistics {
        requireNotNegative("devices", devices);
        requireNotNegative("energyStored", energyStored);
        requireNotNegative("energyCapacity", energyCapacity);
        requireNotNegative("energyInput", energyInput);
        requireNotNegative("energyOutput", energyOutput);
    }

    private static void requireNotNegative(final String name, final long value) {
        if (value < 0) {
            throw new IllegalArgumentException("network statistics " + name + " must not be negative: " + value);
        }
    }
}
