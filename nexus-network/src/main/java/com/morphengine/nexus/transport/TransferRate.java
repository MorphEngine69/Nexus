package com.morphengine.nexus.transport;

/**
 * How often a Puller or Pusher works and how much it moves each time, given the
 * Speed and Stack upgrades it holds. Placeholder balance until the numbers are
 * settled: an operation every {@value #BASE_INTERVAL_TICKS} ticks, each Speed
 * Upgrade {@value #TICKS_PER_SPEED_UPGRADE} ticks sooner, never more often than
 * every {@value #MIN_INTERVAL_TICKS}; a Stack Upgrade moves
 * {@value #STACK_MULTIPLIER} times as much, an Efficiency Upgrade {@value #EFFICIENCY_MULTIPLIER} times, and where
 * both are held the Stack Upgrade counts.
 *
 * @param intervalTicks ticks between two operations, positive
 * @param multiplier    how many steps of a resource one operation moves, positive
 */
public record TransferRate(int intervalTicks, long multiplier) {

    public static final int BASE_INTERVAL_TICKS = 10;
    public static final int TICKS_PER_SPEED_UPGRADE = 2;
    public static final int MIN_INTERVAL_TICKS = 2;
    public static final long STACK_MULTIPLIER = 64;
    public static final long EFFICIENCY_MULTIPLIER = 2;
    public static final TransferRate BASE = of(0, 0);

    public TransferRate {
        if (intervalTicks <= 0 || multiplier <= 0) {
            throw new IllegalArgumentException(
                    "transfer rate needs positive values: interval " + intervalTicks + ", multiplier " + multiplier);
        }
    }

    /**
     * @param speedUpgrades Speed Upgrades held, not negative
     * @param stackUpgrades Stack Upgrades held, not negative; any number counts as one
     */
    public static TransferRate of(final int speedUpgrades, final int stackUpgrades) {
        return of(speedUpgrades, stackUpgrades, 0);
    }

    /**
     * @param speedUpgrades      Speed Upgrades held, not negative
     * @param stackUpgrades      Stack Upgrades held, not negative; any number counts as one
     * @param efficiencyUpgrades Efficiency Upgrades held, not negative; any number counts as one
     */
    public static TransferRate of(
            final int speedUpgrades, final int stackUpgrades, final int efficiencyUpgrades) {
        if (speedUpgrades < 0 || stackUpgrades < 0 || efficiencyUpgrades < 0) {
            throw new IllegalArgumentException("upgrade counts must not be negative: speed " + speedUpgrades
                    + ", stack " + stackUpgrades + ", efficiency " + efficiencyUpgrades);
        }
        final int interval = Math.max(MIN_INTERVAL_TICKS,
                BASE_INTERVAL_TICKS - speedUpgrades * TICKS_PER_SPEED_UPGRADE);
        return new TransferRate(interval, multiplierOf(stackUpgrades, efficiencyUpgrades));
    }

    private static long multiplierOf(final int stackUpgrades, final int efficiencyUpgrades) {
        if (stackUpgrades > 0) {
            return STACK_MULTIPLIER;
        }
        return efficiencyUpgrades > 0 ? EFFICIENCY_MULTIPLIER : 1;
    }
}
