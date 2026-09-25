package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.energy.EnergyBuffer;

/**
 * Average RF/t flowing into and out of an {@link EnergyBuffer}, measured from its
 * lifetime counters between two samples.
 */
public final class EnergyRateMeter {

    private long lastInserted;
    private long lastExtracted;
    private boolean hasBaseline;
    private long inputPerTick;
    private long outputPerTick;

    /**
     * Records the buffer's counters and updates the rates for the elapsed period.
     * The first sample after construction or {@link #rebase} only sets the
     * baseline. Counters that went backwards mean the buffer was replaced; that
     * period is dropped and reported as zero.
     *
     * @param elapsedTicks ticks since the previous sample, must be positive
     */
    public void sample(final EnergyBuffer buffer, final long elapsedTicks) {
        if (elapsedTicks <= 0) {
            throw new IllegalArgumentException("elapsedTicks must be positive: " + elapsedTicks);
        }
        final long inserted = buffer.totalInserted();
        final long extracted = buffer.totalExtracted();
        if (hasBaseline && inserted >= lastInserted && extracted >= lastExtracted) {
            inputPerTick = (inserted - lastInserted) / elapsedTicks;
            outputPerTick = (extracted - lastExtracted) / elapsedTicks;
        } else {
            inputPerTick = 0;
            outputPerTick = 0;
        }
        lastInserted = inserted;
        lastExtracted = extracted;
        hasBaseline = true;
    }

    /**
     * Forgets the previous sample, for when the measured buffer is replaced.
     */
    public void rebase() {
        hasBaseline = false;
    }

    public long inputPerTick() {
        return inputPerTick;
    }

    public long outputPerTick() {
        return outputPerTick;
    }
}
