package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.network.DeviceEnergyUse;

import java.util.Arrays;

/**
 * Counts the energy one device draws, supplies and pays in tolls, and reports it in FE per second averaged over the
 * last {@value #WINDOW_SECONDS} seconds, so that a device that works in bursts, such as an Assembler paying once per
 * run, shows a steady figure instead of one that jumps between its runs and zero.
 *
 * <p>The device records every movement as it happens, which costs no allocation. {@link #sample} closes the seconds
 * that passed since the last call and is meant to be called about once a second; {@link #use} samples first, so any
 * number of readers see the same figures. The first sample only sets the baseline. A gap of several seconds spreads
 * what was counted in it evenly. Server thread only.
 */
public final class DeviceEnergyMeter {

    public static final int WINDOW_SECONDS = 10;
    public static final int TICKS_PER_SECOND = 20;

    private final long[] drawnPerSecond = new long[WINDOW_SECONDS];
    private final long[] suppliedPerSecond = new long[WINDOW_SECONDS];
    private final long[] tollsPerSecond = new long[WINDOW_SECONDS];
    private long drawn;
    private long supplied;
    private long tolls;
    private long drawnAtSample;
    private long suppliedAtSample;
    private long tollsAtSample;
    private long sampleTick;
    private int next;
    private int filled;
    private boolean hasBaseline;

    public void recordDrawn(final long amount) {
        drawn += requirePositive(amount);
    }

    public void recordSupplied(final long amount) {
        supplied += requirePositive(amount);
    }

    public void recordToll(final long amount) {
        tolls += requirePositive(amount);
    }

    /**
     * Closes the whole seconds since the last sample; does nothing while less than a second has passed.
     *
     * @param gameTime the current game time in ticks
     */
    public void sample(final long gameTime) {
        final long elapsed = gameTime - sampleTick;
        if (!hasBaseline || elapsed < 0) {
            restart(gameTime);
            return;
        }
        final int seconds = (int) Math.min(Integer.MAX_VALUE, elapsed / TICKS_PER_SECOND);
        if (seconds == 0) {
            return;
        }
        push(drawnPerSecond, drawn - drawnAtSample, seconds);
        push(suppliedPerSecond, supplied - suppliedAtSample, seconds);
        push(tollsPerSecond, tolls - tollsAtSample, seconds);
        next = (next + Math.min(seconds, WINDOW_SECONDS)) % WINDOW_SECONDS;
        filled = Math.min(WINDOW_SECONDS, filled + seconds);
        drawnAtSample = drawn;
        suppliedAtSample = supplied;
        tollsAtSample = tolls;
        sampleTick += (long) seconds * TICKS_PER_SECOND;
    }

    /**
     * @param gameTime the current game time in ticks
     * @return the average of the seconds measured so far, up to the last {@value #WINDOW_SECONDS}; none until one
     *         whole second has been measured
     */
    public DeviceEnergyUse use(final long gameTime) {
        sample(gameTime);
        if (filled == 0) {
            return DeviceEnergyUse.NONE;
        }
        return new DeviceEnergyUse(average(drawnPerSecond), average(suppliedPerSecond), average(tollsPerSecond));
    }

    private void restart(final long gameTime) {
        drawnAtSample = drawn;
        suppliedAtSample = supplied;
        tollsAtSample = tolls;
        sampleTick = gameTime;
        next = 0;
        filled = 0;
        hasBaseline = true;
        Arrays.fill(drawnPerSecond, 0);
        Arrays.fill(suppliedPerSecond, 0);
        Arrays.fill(tollsPerSecond, 0);
    }

    /**
     * Puts {@code amount}, shared evenly between {@code seconds} seconds, into the newest buckets; the seconds that no
     * longer fit the window are dropped.
     */
    private void push(final long[] buckets, final long amount, final int seconds) {
        final long each = amount / seconds;
        final int kept = Math.min(seconds, WINDOW_SECONDS);
        for (int i = 0; i < kept; i++) {
            buckets[(next + i) % WINDOW_SECONDS] = each;
        }
    }

    private long average(final long[] buckets) {
        long sum = 0;
        for (int i = 1; i <= filled; i++) {
            sum += buckets[Math.floorMod(next - i, WINDOW_SECONDS)];
        }
        return sum / filled;
    }

    private static long requirePositive(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("energy movement must be positive: " + amount);
        }
        return amount;
    }
}
