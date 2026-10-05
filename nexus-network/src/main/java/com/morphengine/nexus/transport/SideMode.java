package com.morphengine.nexus.transport;

/**
 * What a side of a block lets other blocks do through it.
 */
public enum SideMode {

    /** Nothing goes in or out. */
    CLOSED(false, false),
    /** Other blocks may put resources in. */
    INPUT(true, false),
    /** Other blocks may take resources out. */
    OUTPUT(false, true),
    /** Other blocks may put resources in and take them out. */
    BOTH(true, true);

    private static final SideMode[] VALUES = values();

    private final boolean allowsInput;
    private final boolean allowsOutput;

    SideMode(final boolean allowsInput, final boolean allowsOutput) {
        this.allowsInput = allowsInput;
        this.allowsOutput = allowsOutput;
    }

    public boolean allowsInput() {
        return allowsInput;
    }

    public boolean allowsOutput() {
        return allowsOutput;
    }

    /**
     * @return the mode a click moves to: closed, input, output, both and round again
     */
    public SideMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /**
     * @return the mode a click the other way moves to
     */
    public SideMode previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }

    /**
     * @param code {@link #ordinal()} of a mode; any other number gives {@link #CLOSED}, so that a damaged value
     *             closes a side rather than opening it
     */
    public static SideMode ofOrdinal(final int code) {
        return code >= 0 && code < VALUES.length ? VALUES[code] : CLOSED;
    }
}
