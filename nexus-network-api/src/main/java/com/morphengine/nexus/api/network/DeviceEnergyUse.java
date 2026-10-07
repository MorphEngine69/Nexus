package com.morphengine.nexus.api.network;

/**
 * What one device did with energy over the last measured period, in FE per second.
 *
 * @param drawn    FE the device took for its work: a machine topping up its buffer, a Pusher giving FE away
 * @param supplied FE the device gave to the network: a generator, a Puller taking FE in
 * @param tolls    FE the network paid for the operations the device did
 */
public record DeviceEnergyUse(long drawn, long supplied, long tolls) {

    public static final DeviceEnergyUse NONE = new DeviceEnergyUse(0, 0, 0);

    public DeviceEnergyUse {
        requireNotNegative("drawn", drawn);
        requireNotNegative("supplied", supplied);
        requireNotNegative("tolls", tolls);
    }

    /**
     * @return everything the device took from the energy of its network: its work and its operations
     */
    public long spent() {
        return drawn + tolls;
    }

    private static void requireNotNegative(final String name, final long value) {
        if (value < 0) {
            throw new IllegalArgumentException("device energy use " + name + " must not be negative: " + value);
        }
    }
}
