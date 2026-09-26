package com.morphengine.nexus.terminal;

/**
 * Steps through the values of an enum in a circle, as a side button does on
 * each click.
 */
public final class EnumCycle {

    private EnumCycle() {
    }

    /**
     * @param backwards step to the previous value instead of the next
     */
    public static <E extends Enum<E>> E step(final E value, final boolean backwards) {
        final E[] values = value.getDeclaringClass().getEnumConstants();
        final int offset = backwards ? values.length - 1 : 1;
        return values[(value.ordinal() + offset) % values.length];
    }
}
