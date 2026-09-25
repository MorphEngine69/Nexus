package com.morphengine.nexus.api.energy;

import com.morphengine.nexus.api.core.Action;

/**
 * A store of RF. All amounts are in RF and never negative. Implementations are
 * not thread-safe and are used from the server thread only.
 */
public interface EnergyBuffer {

    long stored();

    long capacity();

    /**
     * Inserts energy, limited by free space and by the buffer's insert rate.
     *
     * @param amount RF offered, must not be negative
     * @return RF accepted, never greater than {@code amount}; under
     *         {@link Action#SIMULATE} the amount that would be accepted
     */
    long insert(long amount, Action action);

    /**
     * Extracts energy, limited by what is stored and by the buffer's extract rate.
     *
     * @param amount RF requested, must not be negative
     * @return RF removed, never greater than {@code amount}; under
     *         {@link Action#SIMULATE} the amount that would be removed
     */
    long extract(long amount, Action action);

    /**
     * RF accepted by executed inserts since the buffer was created or restored.
     * Only grows; used to measure throughput, not persisted.
     */
    long totalInserted();

    /**
     * RF removed by executed extracts since the buffer was created or restored.
     * Only grows; used to measure throughput, not persisted.
     */
    long totalExtracted();
}
