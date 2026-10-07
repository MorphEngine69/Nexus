package com.morphengine.nexus.storage;

/**
 * When a block outside the network, such as a chest, is read again. A small block is read every second; a larger one
 * is read a little less often, as reading it costs more, one more tick of waiting for every {@value #ENTRIES_PER_TICK}
 * entries it listed, but never less often than every {@value #MAX_INTERVAL_TICKS} ticks, so that what the network
 * knows of it is never long out of date. While someone may be changing it by hand, see {@link #watch}, it is read
 * every {@value #WATCH_INTERVAL_TICKS} ticks. Not thread safe; one per block.
 */
public final class ScanSchedule {

    public static final int BASE_INTERVAL_TICKS = 20;
    public static final int MAX_INTERVAL_TICKS = 40;
    public static final int ENTRIES_PER_TICK = 8;
    public static final int WATCH_INTERVAL_TICKS = 5;
    public static final int WATCH_DURATION_TICKS = 600;

    private int entries;
    private long watchedUntil = -1;

    /**
     * @param now the current game time in ticks
     * @return ticks to wait before the next read
     */
    public int intervalTicks(final long now) {
        if (now < watchedUntil) {
            return WATCH_INTERVAL_TICKS;
        }
        return Math.min(MAX_INTERVAL_TICKS, BASE_INTERVAL_TICKS + entries / ENTRIES_PER_TICK);
    }

    /**
     * Takes in the result of a read, which says how large the block is.
     */
    public void scanned(final ExternalStorage.Scan scan) {
        entries = scan.entries();
    }

    /**
     * Has the block read often for the next {@value #WATCH_DURATION_TICKS} ticks, for when a player is at it.
     *
     * @param now the current game time in ticks
     */
    public void watch(final long now) {
        watchedUntil = now + WATCH_DURATION_TICKS;
    }
}
