package com.morphengine.nexus.energy;

/**
 * The upgrades of a device that change what its operations cost.
 *
 * @param speed      Speed Upgrades, not negative; more than {@link OperationPrice#MAX_SPEED_UPGRADES} count as the most
 * @param efficiency Efficiency Upgrades, not negative; more than {@link EfficiencyUpgrades#MAX_UPGRADES} count as the
 *                   most
 */
public record OperationUpgrades(int speed, int efficiency) {

    public static final OperationUpgrades NONE = new OperationUpgrades(0, 0);

    public OperationUpgrades {
        if (speed < 0 || efficiency < 0) {
            throw new IllegalArgumentException(
                    "upgrade counts must not be negative: speed " + speed + ", efficiency " + efficiency);
        }
    }

    /**
     * @return the upgrades of a device that holds only Speed Upgrades
     */
    public static OperationUpgrades speedOnly(final int speed) {
        return new OperationUpgrades(speed, 0);
    }
}
