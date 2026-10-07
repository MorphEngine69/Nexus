package com.morphengine.nexus.energy;

import java.util.Objects;

/**
 * What an operation costs in FE: its base cost, times a factor that grows with the number of devices in the network,
 * times a factor that doubles with every Speed Upgrade of the device that does it. A large network and a fast device
 * both pay for what they ask of the network. Placeholder balance until the numbers are settled.
 */
public final class OperationPrice {

    /** Speed Upgrades a device may hold at most; the price of one more would not be defined. */
    public static final int MAX_SPEED_UPGRADES = 4;

    private static final int PERCENT = 100;
    private static final int[] SIZE_DEVICES = {10, 50, 100, 250, 500};
    private static final double[] SIZE_FACTOR = {1.0, 1.5, 2.0, 3.5, 6.0};

    private OperationPrice() {
    }

    /**
     * @param devices       devices in the network, not negative
     * @param speedUpgrades Speed Upgrades of the device that works, from zero to {@link #MAX_SPEED_UPGRADES}
     * @return FE the operation costs, never less than one
     */
    public static long of(final OperationKind kind, final int devices, final int speedUpgrades) {
        return of(kind, devices, OperationUpgrades.speedOnly(speedUpgrades));
    }

    /**
     * @param devices  devices in the network, not negative
     * @param upgrades the upgrades of the device that works; an Efficiency Upgrade takes its share off the price
     * @return FE the operation costs, never less than one
     */
    public static long of(final OperationKind kind, final int devices, final OperationUpgrades upgrades) {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(upgrades, "upgrades must not be null");
        final int efficiency = Math.min(upgrades.efficiency(), EfficiencyUpgrades.MAX_UPGRADES);
        final long speedFactor = speedFactor(Math.min(upgrades.speed(), MAX_SPEED_UPGRADES));
        final double price = kind.baseCost() * sizeFactor(devices) * speedFactor
                * EfficiencyUpgrades.costPercent(efficiency) / PERCENT;
        return Math.max(1, Math.round(price));
    }

    /**
     * @param devices devices in the network, not negative
     * @return what the size of the network makes things cost: one for a small network, rising evenly between the
     *         sizes it is set for, and the highest it is set for beyond the largest
     */
    public static double sizeFactor(final int devices) {
        if (devices < 0) {
            throw new IllegalArgumentException("devices must not be negative: " + devices);
        }
        if (devices <= SIZE_DEVICES[0]) {
            return SIZE_FACTOR[0];
        }
        for (int step = 1; step < SIZE_DEVICES.length; step++) {
            if (devices <= SIZE_DEVICES[step]) {
                final double share = (double) (devices - SIZE_DEVICES[step - 1])
                        / (SIZE_DEVICES[step] - SIZE_DEVICES[step - 1]);
                return SIZE_FACTOR[step - 1] + share * (SIZE_FACTOR[step] - SIZE_FACTOR[step - 1]);
            }
        }
        return SIZE_FACTOR[SIZE_FACTOR.length - 1];
    }

    /**
     * @param speedUpgrades Speed Upgrades of the device, from zero to {@link #MAX_SPEED_UPGRADES}
     * @return what the Speed Upgrades make the work cost: double for each
     */
    public static long speedFactor(final int speedUpgrades) {
        if (speedUpgrades < 0 || speedUpgrades > MAX_SPEED_UPGRADES) {
            throw new IllegalArgumentException("speed upgrades must be 0 to " + MAX_SPEED_UPGRADES + ": "
                    + speedUpgrades);
        }
        return 1L << speedUpgrades;
    }
}
