package com.morphengine.nexus.menu;

import org.jspecify.annotations.Nullable;

/**
 * What the Coal Generator panel shows. Energy in RF, production in RF per tick.
 *
 * @param production     RF made in the last tick; zero when idle or paused
 * @param burnTicksLeft  ticks the current piece of fuel still burns
 * @param burnTicksTotal ticks the current piece of fuel burns in total
 * @param network        the network the generator feeds; {@code null} when no Nexus is connected
 */
public record CoalGeneratorView(
        long stored, long capacity, long production, int burnTicksLeft, int burnTicksTotal,
        @Nullable NetworkBadge network) {

    public static final CoalGeneratorView EMPTY = new CoalGeneratorView(0, 0, 0, 0, 0, null);

    public Status status() {
        if (production > 0) {
            return Status.GENERATING;
        }
        return burnTicksLeft > 0 ? Status.BUFFER_FULL : Status.NO_FUEL;
    }

    public enum Status {
        GENERATING,
        BUFFER_FULL,
        NO_FUEL
    }
}
