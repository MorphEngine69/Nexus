package com.morphengine.nexus.api.network;

import java.util.Map;
import java.util.Objects;

/**
 * Figures shown in the Nexus interface. Energy values are in RF, rates in RF per tick.
 *
 * @param devices        devices connected to the Nexus; cables and the Nexus itself are not counted
 * @param devicesByRole  the same devices by what they do; a role left out counts zero; copied
 * @param energyStored   RF held by all energy buffers of the network
 * @param energyCapacity RF all energy buffers of the network can hold
 * @param energyInput    average RF/t entering the network's buffers
 * @param energyOutput   average RF/t leaving the network's buffers
 */
public record NetworkStatistics(
        int devices, Map<DeviceRole, Integer> devicesByRole, long energyStored, long energyCapacity,
        long energyInput, long energyOutput) {

    public static final NetworkStatistics EMPTY = new NetworkStatistics(0, Map.of(), 0, 0, 0, 0);

    public NetworkStatistics {
        requireNotNegative("devices", devices);
        devicesByRole = Map.copyOf(Objects.requireNonNull(devicesByRole, "devicesByRole must not be null"));
        for (Map.Entry<DeviceRole, Integer> count : devicesByRole.entrySet()) {
            requireNotNegative(count.getKey() + " devices", count.getValue());
        }
        requireNotNegative("energyStored", energyStored);
        requireNotNegative("energyCapacity", energyCapacity);
        requireNotNegative("energyInput", energyInput);
        requireNotNegative("energyOutput", energyOutput);
    }

    /**
     * @return devices of the network that do {@code role}
     */
    public int count(final DeviceRole role) {
        return devicesByRole.getOrDefault(role, 0);
    }

    private static void requireNotNegative(final String name, final long value) {
        if (value < 0) {
            throw new IllegalArgumentException("network statistics " + name + " must not be negative: " + value);
        }
    }
}
