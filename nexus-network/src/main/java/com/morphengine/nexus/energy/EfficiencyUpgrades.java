package com.morphengine.nexus.energy;

/**
 * What an Efficiency Upgrade does. A machine pays less FE for the work: the upgrade takes {@value #COST_STEP_PERCENT}
 * percent off the price. A generator gets more FE out of the same fuel: it adds {@value #YIELD_STEP_PERCENT} percent.
 * Placeholder balance until the numbers are settled.
 */
public final class EfficiencyUpgrades {

    public static final int MAX_UPGRADES = 1;

    private static final int PERCENT = 100;
    private static final int COST_STEP_PERCENT = 30;
    private static final int YIELD_STEP_PERCENT = 30;

    private EfficiencyUpgrades() {
    }

    /**
     * @param upgrades Efficiency Upgrades in the machine, from zero to {@link #MAX_UPGRADES}
     * @return the share of the price of the work the machine pays, in percent
     */
    public static int costPercent(final int upgrades) {
        return PERCENT - COST_STEP_PERCENT * requireInRange(upgrades);
    }

    /**
     * @param energy   FE the fuel gives without upgrades, not negative
     * @param upgrades Efficiency Upgrades in the generator, from zero to {@link #MAX_UPGRADES}
     * @return the FE the fuel gives with them, rounded down
     */
    public static long yielded(final long energy, final int upgrades) {
        return energy * (PERCENT + YIELD_STEP_PERCENT * requireInRange(upgrades)) / PERCENT;
    }

    private static int requireInRange(final int upgrades) {
        if (upgrades < 0 || upgrades > MAX_UPGRADES) {
            throw new IllegalArgumentException("Efficiency Upgrades must be from 0 to " + MAX_UPGRADES + ": "
                    + upgrades);
        }
        return upgrades;
    }
}
