package com.morphengine.nexus.machine;

import java.util.Optional;

/**
 * How fast a machine works given its tier and the Speed Upgrades in it. Placeholder balance: each Speed Upgrade adds
 * a share of the way to the speed of the next tier, and all of them together bring a machine to
 * {@value #LIMIT_PERCENT} percent of the way, never to the next tier. Quantum, with no tier above it, gains
 * {@value #LAST_TIER_BONUS_PERCENT} percent of its own speed.
 */
public final class MachineSpeed {

    public static final int MAX_SPEED_UPGRADES = 4;
    public static final int LIMIT_PERCENT = 90;
    public static final int LAST_TIER_BONUS_PERCENT = 50;

    private static final int PERCENT = 100;

    private MachineSpeed() {
    }

    /**
     * @param speedUpgrades Speed Upgrades in the machine, from zero to {@link #MAX_SPEED_UPGRADES}
     * @return the speed in percent of the time of a recipe, 100 being the time the recipe lists
     */
    public static int percent(final MachineTier tier, final int speedUpgrades) {
        if (speedUpgrades < 0 || speedUpgrades > MAX_SPEED_UPGRADES) {
            throw new IllegalArgumentException("Speed Upgrades must be from 0 to " + MAX_SPEED_UPGRADES + ": "
                    + speedUpgrades);
        }
        final Optional<MachineTier> next = tier.next();
        final int reach = next.map(above -> (above.speedPercent() - tier.speedPercent()) * LIMIT_PERCENT / PERCENT)
                .orElse(tier.speedPercent() * LAST_TIER_BONUS_PERCENT / PERCENT);
        return tier.speedPercent() + reach * speedUpgrades / MAX_SPEED_UPGRADES;
    }
}
