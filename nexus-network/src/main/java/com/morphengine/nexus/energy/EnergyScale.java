package com.morphengine.nexus.energy;

/**
 * Scales energy figures by a share set in percent, as the settings of a world do for what generators make, what
 * machines use and what operations cost.
 */
public final class EnergyScale {

    /** The share that changes nothing. */
    public static final int NEUTRAL_PERCENT = 100;
    /** The smallest and largest share a setting may ask for. */
    public static final int MIN_PERCENT = 1;
    public static final int MAX_PERCENT = 10_000;

    private EnergyScale() {
    }

    /**
     * @param multiplier a factor such as 1.5; clamped to what {@link #MIN_PERCENT} and {@link #MAX_PERCENT} allow
     * @return the same factor as a share in percent, rounded
     */
    public static int percentOf(final double multiplier) {
        if (Double.isNaN(multiplier)) {
            return NEUTRAL_PERCENT;
        }
        return (int) Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, Math.round(multiplier * NEUTRAL_PERCENT)));
    }

    /**
     * @param base    an amount of FE, not negative
     * @param percent the share to take of it, positive
     * @return the share of {@code base}, rounded down, never above {@link Long#MAX_VALUE}
     */
    public static long of(final long base, final int percent) {
        requireShare(percent);
        if (base < 0) {
            throw new IllegalArgumentException("base must not be negative: " + base);
        }
        if (percent == NEUTRAL_PERCENT) {
            return base;
        }
        final long whole = base / NEUTRAL_PERCENT;
        final long rest = base % NEUTRAL_PERCENT;
        final long fromWhole = whole > Long.MAX_VALUE / percent ? Long.MAX_VALUE : whole * percent;
        final long fromRest = rest * percent / NEUTRAL_PERCENT;
        return fromWhole > Long.MAX_VALUE - fromRest ? Long.MAX_VALUE : fromWhole + fromRest;
    }

    /**
     * @return the share of a price, rounded, and never less than one, so that a price is never made nothing
     */
    public static long priceOf(final long base, final int percent) {
        requireShare(percent);
        if (percent == NEUTRAL_PERCENT) {
            return base;
        }
        return Math.max(1, Math.round((double) base * percent / NEUTRAL_PERCENT));
    }

    private static void requireShare(final int percent) {
        if (percent < MIN_PERCENT || percent > MAX_PERCENT) {
            throw new IllegalArgumentException(
                    "percent must be " + MIN_PERCENT + " to " + MAX_PERCENT + ": " + percent);
        }
    }
}
