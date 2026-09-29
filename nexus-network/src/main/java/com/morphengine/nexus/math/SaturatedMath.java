package com.morphengine.nexus.math;

/**
 * Arithmetic on amounts that stops at {@link Long#MAX_VALUE} instead of
 * overflowing, for sums and products of capacities that may exceed a
 * {@code long}. Operands must not be negative.
 */
public final class SaturatedMath {

    private SaturatedMath() {
    }

    public static long add(final long left, final long right) {
        final long sum = left + right;
        return sum < left ? Long.MAX_VALUE : sum;
    }

    public static long multiply(final long left, final long right) {
        final long high = Math.multiplyHigh(left, right);
        final long product = left * right;
        return high == 0 && product >= 0 ? product : Long.MAX_VALUE;
    }
}
