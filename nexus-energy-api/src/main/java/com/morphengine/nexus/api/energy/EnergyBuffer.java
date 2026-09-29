package com.morphengine.nexus.api.energy;

import com.morphengine.nexus.api.core.Action;

/**
 * A store of FE. All amounts are in FE and never negative. Implementations are
 * not thread-safe and are used from the server thread only.
 */
public interface EnergyBuffer {

    long stored();

    long capacity();

    /**
     * Inserts energy, limited by free space and by the buffer's insert rate.
     *
     * @param amount FE offered, must not be negative
     * @return FE accepted, never greater than {@code amount}; under
     *         {@link Action#SIMULATE} the amount that would be accepted
     */
    long insert(long amount, Action action);

    /**
     * Extracts energy, limited by what is stored and by the buffer's extract rate.
     *
     * @param amount FE requested, must not be negative
     * @return FE removed, never greater than {@code amount}; under
     *         {@link Action#SIMULATE} the amount that would be removed
     */
    long extract(long amount, Action action);

    /**
     * FE accepted by executed inserts since the buffer was created or restored.
     * Only grows; used to measure throughput, not persisted.
     */
    long totalInserted();

    /**
     * FE removed by executed extracts since the buffer was created or restored.
     * Only grows; used to measure throughput, not persisted.
     */
    long totalExtracted();
}
