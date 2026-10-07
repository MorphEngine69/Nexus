package com.morphengine.nexus.item;

import com.morphengine.nexus.api.storage.CellSpec;

/**
 * Size of a Vault Cell. Placeholder balance until the numbers are settled: item cells hold a
 * little less than the cells of AE2 and fewer kinds of items in the small sizes; fluid and
 * energy cells have the byte sizes of AE2. Each type reserves 1/128 of the cell.
 */
public enum CellTier {

    ONE_K("1k", 1, 512, 32),
    FOUR_K("4k", 4, 2_048, 32),
    SIXTEEN_K("16k", 16, 8_192, 48),
    SIXTY_FOUR_K("64k", 64, 32_768, 48),
    TWO_HUNDRED_FIFTY_SIX_K("256k", 256, 131_072, 63),
    FIVE_HUNDRED_TWELVE_K("512k", 512, 524_288, 63);

    private static final long BYTES_PER_KILOBYTE = 1024;
    private static final long TYPE_SHARE = 128;

    private final String label;
    private final long kilobytes;
    private final long itemBytes;
    private final int itemTypes;

    CellTier(final String label, final long kilobytes, final long itemBytes, final int itemTypes) {
        this.label = label;
        this.kilobytes = kilobytes;
        this.itemBytes = itemBytes;
        this.itemTypes = itemTypes;
    }

    /**
     * @return the size as players read it, such as {@code 4k}; also part of item ids
     */
    public String label() {
        return label;
    }

    public CellSpec specFor(final CellKind kind) {
        final boolean items = kind == CellKind.ITEM;
        final long bytes = items ? itemBytes : kilobytes * BYTES_PER_KILOBYTE;
        return new CellSpec(bytes, bytes / TYPE_SHARE, items ? itemTypes : kind.maxTypes(), kind.unitsPerByte());
    }
}
