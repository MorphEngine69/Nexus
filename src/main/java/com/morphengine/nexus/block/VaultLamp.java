package com.morphengine.nexus.block;

import com.morphengine.nexus.api.storage.CellStatus;
import org.jspecify.annotations.Nullable;

/**
 * The lamp beside one cell of a Storage Vault. All lamps of a vault are packed
 * into one {@code long}, two bits per slot, so the client gets them in a single
 * value.
 */
public enum VaultLamp {

    /** No cell in the slot, or no energy in the network. */
    OFF,

    /** The cell has room. */
    GREEN,

    /** The cell has run out of types or of bytes. */
    ORANGE,

    /** The cell has run out of both. */
    RED;

    private static final int BITS = 2;
    private static final long MASK = (1L << BITS) - 1;
    private static final VaultLamp[] VALUES = values();

    public static VaultLamp of(final @Nullable CellStatus status) {
        if (status == null) {
            return OFF;
        }
        return switch (status) {
            case HAS_ROOM -> GREEN;
            case TYPES_FULL, BYTES_FULL -> ORANGE;
            case FULL -> RED;
        };
    }

    /**
     * @param slot from zero to 31: a {@code long} holds the lamps of up to 32 slots
     * @return {@code lamps} with the lamp of {@code slot} set to this one
     */
    public long packInto(final long lamps, final int slot) {
        final int shift = slot * BITS;
        return lamps & ~(MASK << shift) | (long) ordinal() << shift;
    }

    public static VaultLamp unpack(final long lamps, final int slot) {
        return VALUES[(int) (lamps >>> slot * BITS & MASK)];
    }
}
