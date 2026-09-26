package com.morphengine.nexus.item;

import com.morphengine.nexus.api.storage.CellSpec;

/**
 * Size of a Vault Cell. Placeholder balance until the numbers are settled: the
 * byte sizes of AE2, with each type reserving 1/128 of the cell, so a full set
 * of types takes half of it.
 */
public enum CellTier {

    ONE_K("1k", 1),
    FOUR_K("4k", 4),
    SIXTEEN_K("16k", 16),
    SIXTY_FOUR_K("64k", 64),
    TWO_HUNDRED_FIFTY_SIX_K("256k", 256),
    FIVE_HUNDRED_TWELVE_K("512k", 512);

    private static final long BYTES_PER_KILOBYTE = 1024;
    private static final long TYPE_SHARE = 128;

    private final String label;
    private final long kilobytes;

    CellTier(final String label, final long kilobytes) {
        this.label = label;
        this.kilobytes = kilobytes;
    }

    /**
     * @return the size as players read it, such as {@code 4k}; also part of item ids
     */
    public String label() {
        return label;
    }

    public CellSpec specFor(final CellKind kind) {
        final long bytes = kilobytes * BYTES_PER_KILOBYTE;
        return new CellSpec(bytes, bytes / TYPE_SHARE, kind.maxTypes(), kind.unitsPerByte());
    }
}
