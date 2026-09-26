package com.morphengine.nexus.api.storage;

/**
 * How full a storage cell is, as shown by its lamp.
 */
public enum CellStatus {

    /** Room for more of the stored resources and for new ones. */
    HAS_ROOM,

    /** No new resource fits, but more of the stored ones does. */
    TYPES_FULL,

    /** Not one more unit fits, though a new kind of resource would still have a free type. */
    BYTES_FULL,

    /** Neither a new resource nor one more unit fits. */
    FULL;

    public static CellStatus of(final boolean typesFull, final boolean bytesFull) {
        if (typesFull) {
            return bytesFull ? FULL : TYPES_FULL;
        }
        return bytesFull ? BYTES_FULL : HAS_ROOM;
    }
}
