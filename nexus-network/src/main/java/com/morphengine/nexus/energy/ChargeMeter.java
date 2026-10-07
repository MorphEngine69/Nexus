package com.morphengine.nexus.energy;

/**
 * Turns how much a buffer holds into the number of segments of a bar to light: the nearest segment, but never
 * all of them before the buffer is full, and never none while it holds anything.
 */
public final class ChargeMeter {

    private ChargeMeter() {
    }

    /**
     * @param stored   FE held; a value outside {@code 0..capacity} counts as the nearer end
     * @param capacity FE the buffer holds at most, must be positive
     * @param segments how many segments the bar has, must be positive
     * @return the segments to light, from {@code 0} (empty) to {@code segments} (full)
     */
    public static int segments(final long stored, final long capacity, final int segments) {
        if (capacity <= 0 || segments <= 0) {
            throw new IllegalArgumentException(
                    "a charge meter needs positive values: capacity=" + capacity + ", segments=" + segments);
        }
        if (stored <= 0) {
            return 0;
        }
        if (stored >= capacity) {
            return segments;
        }
        final long nearest = Math.round((double) stored / capacity * segments);
        return (int) Math.max(1, Math.min(segments - 1, nearest));
    }
}
