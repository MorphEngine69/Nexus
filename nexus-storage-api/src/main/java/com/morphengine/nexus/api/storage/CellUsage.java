package com.morphengine.nexus.api.storage;

import java.util.Objects;

/**
 * How much of a storage cell is taken, as shown to players.
 *
 * @param usedBytes   bytes taken by stored units and by the types reserved, from zero to {@code totalBytes}
 * @param totalBytes  bytes the cell holds
 * @param storedTypes distinct resources stored, from zero to {@code maxTypes}
 * @param maxTypes    distinct resources the cell holds at most
 */
public record CellUsage(long usedBytes, long totalBytes, int storedTypes, int maxTypes, CellStatus status) {

    public CellUsage {
        Objects.requireNonNull(status, "status must not be null");
        if (usedBytes < 0 || usedBytes > totalBytes || storedTypes < 0 || storedTypes > maxTypes) {
            throw new IllegalArgumentException(String.format(
                    "cell usage out of range: bytes %d of %d, types %d of %d",
                    usedBytes, totalBytes, storedTypes, maxTypes));
        }
    }
}
