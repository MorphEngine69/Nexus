package com.morphengine.nexus.api.storage;

/**
 * Capacity of a storage cell in the byte model. Stored units share bytes, and
 * every distinct resource reserves bytes of its own on top, so the more kinds a
 * cell holds, the less of them fits in total.
 *
 * @param totalBytes   bytes the cell holds, positive
 * @param bytesPerType bytes each distinct resource reserves, positive and at most {@code totalBytes}
 * @param maxTypes     distinct resources the cell holds at most, positive
 * @param unitsPerByte units of a resource one byte holds, positive, such as 8 items
 */
public record CellSpec(long totalBytes, long bytesPerType, int maxTypes, long unitsPerByte) {

    public CellSpec {
        if (totalBytes <= 0 || bytesPerType <= 0 || maxTypes <= 0 || unitsPerByte <= 0) {
            throw new IllegalArgumentException("cell spec needs positive values: " + describe(
                    totalBytes, bytesPerType, maxTypes, unitsPerByte));
        }
        if (bytesPerType > totalBytes) {
            throw new IllegalArgumentException("a single type does not fit the cell: " + describe(
                    totalBytes, bytesPerType, maxTypes, unitsPerByte));
        }
    }

    private static String describe(
            final long totalBytes, final long bytesPerType, final int maxTypes, final long unitsPerByte) {
        return String.format("totalBytes=%d, bytesPerType=%d, maxTypes=%d, unitsPerByte=%d",
                totalBytes, bytesPerType, maxTypes, unitsPerByte);
    }
}
