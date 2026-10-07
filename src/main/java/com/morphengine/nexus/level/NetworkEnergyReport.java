package com.morphengine.nexus.level;

import com.morphengine.nexus.api.network.DeviceEnergyUse;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Who draws and who supplies energy in a network: its devices, and the terminals that carry no block of their own.
 *
 * @param portableTerminals what the portable terminals paid in tolls; they stand in no place of the network
 * @param devices           the devices of the network that keep count; copied
 */
public record NetworkEnergyReport(DeviceEnergyUse portableTerminals, List<DeviceEnergyRow> devices) {

    public static final NetworkEnergyReport EMPTY = new NetworkEnergyReport(DeviceEnergyUse.NONE, List.of());

    public NetworkEnergyReport {
        Objects.requireNonNull(portableTerminals, "portableTerminals must not be null");
        devices = List.copyOf(Objects.requireNonNull(devices, "devices must not be null"));
    }

    /**
     * @return the report with at most {@code max} devices, the ones that move the most energy kept
     */
    public NetworkEnergyReport limitedTo(final int max) {
        if (devices.size() <= max) {
            return this;
        }
        final Comparator<DeviceEnergyRow> byActivity = Comparator.comparingLong(
                row -> -(row.use().spent() + row.use().supplied()));
        return new NetworkEnergyReport(portableTerminals, devices.stream().sorted(byActivity).limit(max).toList());
    }
}
