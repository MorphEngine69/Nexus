package com.morphengine.nexus.level;

import com.morphengine.nexus.energy.DeviceEnergyMeter;

import java.util.ArrayList;
import java.util.List;

/**
 * Who draws and who supplies energy in one network: the devices that keep count, as the last rebuild found them, and
 * the meter that takes the tolls of the portable terminals. Server thread only.
 */
public final class EnergyAccountComponent implements NetworkComponent {

    private final DeviceEnergyMeter portableTerminals = new DeviceEnergyMeter();
    private List<MeteredDevice> devices = List.of();

    @Override
    public void adopt(final List<NetworkMember> members) {
        final List<MeteredDevice> metered = new ArrayList<>();
        for (NetworkMember member : members) {
            if (member instanceof MeteredDevice device) {
                metered.add(device);
            }
        }
        devices = metered;
    }

    /**
     * @return the meter that records the tolls of terminals that have no block, which the portable ones do
     */
    public DeviceEnergyMeter portableTerminals() {
        return portableTerminals;
    }

    /**
     * Closes the second just passed in the meter of every device, so that a figure is ready whenever it is asked for.
     * Allocation free.
     *
     * @param gameTime the current game time in ticks
     */
    public void sample(final long gameTime) {
        portableTerminals.sample(gameTime);
        for (int i = 0; i < devices.size(); i++) {
            devices.get(i).energyMeter().sample(gameTime);
        }
    }

    /**
     * @param gameTime the current game time in ticks
     */
    public NetworkEnergyReport report(final long gameTime) {
        final List<DeviceEnergyRow> rows = new ArrayList<>(devices.size());
        for (MeteredDevice device : devices) {
            rows.add(device.energyRow(gameTime));
        }
        return new NetworkEnergyReport(portableTerminals.use(gameTime), rows);
    }
}
