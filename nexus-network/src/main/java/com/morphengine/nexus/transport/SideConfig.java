package com.morphengine.nexus.transport;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link SideMode} of every side of a block, which the player sets one by one. Immutable. The sides are an enum
 * the block picks: the six faces of a cell, the six sides of a machine counted from its front.
 *
 * @param <K> the sides
 */
public final class SideConfig<K extends Enum<K>> {

    /** Bits that hold the mode of one side; {@link #toBits} fits as many sides as an int has pairs of bits. */
    private static final int BITS_PER_SIDE = 2;
    private static final int MODE_MASK = (1 << BITS_PER_SIDE) - 1;
    private static final int MAX_SIDES = Integer.SIZE / BITS_PER_SIDE;

    private final Class<K> sides;
    private final Map<K, SideMode> modes;

    private SideConfig(final Class<K> sides, final Map<K, SideMode> modes) {
        this.sides = sides;
        this.modes = modes;
    }

    /**
     * @return a configuration with every side closed
     * @throws IllegalArgumentException if {@code sides} has more constants than fit in {@link #toBits}
     */
    public static <K extends Enum<K>> SideConfig<K> closed(final Class<K> sides) {
        Objects.requireNonNull(sides, "sides must not be null");
        if (sides.getEnumConstants().length > MAX_SIDES) {
            throw new IllegalArgumentException("too many sides for one int: " + sides.getEnumConstants().length);
        }
        final Map<K, SideMode> modes = new EnumMap<>(sides);
        for (K side : sides.getEnumConstants()) {
            modes.put(side, SideMode.CLOSED);
        }
        return new SideConfig<>(sides, modes);
    }

    /**
     * @param bits what {@link #toBits} gave; bits of sides that do not exist are ignored, and a damaged mode closes
     *             its side
     */
    public static <K extends Enum<K>> SideConfig<K> fromBits(final Class<K> sides, final int bits) {
        SideConfig<K> config = closed(sides);
        for (K side : sides.getEnumConstants()) {
            config = config.with(side, SideMode.ofOrdinal(bits >> side.ordinal() * BITS_PER_SIDE & MODE_MASK));
        }
        return config;
    }

    public SideMode mode(final K side) {
        return modes.get(Objects.requireNonNull(side, "side must not be null"));
    }

    /**
     * @return this configuration with {@code side} in {@code mode}; this one is not changed
     */
    public SideConfig<K> with(final K side, final SideMode mode) {
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        if (modes.get(side) == mode) {
            return this;
        }
        final Map<K, SideMode> changed = new EnumMap<>(modes);
        changed.put(side, mode);
        return new SideConfig<>(sides, changed);
    }

    /**
     * @return the configuration as one number, two bits for every side, to save and to send
     */
    public int toBits() {
        int bits = 0;
        for (Map.Entry<K, SideMode> entry : modes.entrySet()) {
            bits |= entry.getValue().ordinal() << entry.getKey().ordinal() * BITS_PER_SIDE;
        }
        return bits;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof SideConfig<?> config && sides == config.sides && modes.equals(config.modes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sides, modes);
    }

    @Override
    public String toString() {
        return "SideConfig" + modes;
    }
}
