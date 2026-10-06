package com.morphengine.nexus.menu;

import com.morphengine.nexus.resource.FluidKey;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a generator panel shows. Energy in FE, production in FE per tick, tanks in millibuckets.
 *
 * @param production     FE made in the last tick; zero when idle or paused
 * @param burnTicksLeft  ticks the current piece of fuel still burns
 * @param burnTicksTotal ticks the current piece of fuel burns in total
 * @param network        the network the generator feeds; {@code null} when no Nexus is connected
 * @param tanks          the tanks of a generator that burns fluids, in the order of its panel; none for the others
 * @param accepted       the fluid each tank takes, in the same order, to say what to pour into an empty one
 */
public record GeneratorView(
        long stored, long capacity, long production, int burnTicksLeft, int burnTicksTotal,
        @Nullable NetworkBadge network, List<TankView> tanks,
        List<FluidKey> accepted) {

    public static final GeneratorView EMPTY = new GeneratorView(0, 0, 0, 0, 0, null, List.of(), List.of());

    public GeneratorView {
        tanks = List.copyOf(tanks);
        accepted = List.copyOf(accepted);
    }

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
