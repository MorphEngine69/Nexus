package com.morphengine.nexus.machine;

import java.util.List;
import java.util.Optional;

/**
 * A tier of a machine: Basic, Advanced, Superior and Quantum. The numbers are placeholder balance until the numbers
 * are settled.
 *
 * @param rank           1 for Basic up to 4 for Quantum
 * @param linePairs      input slots, and as many output slots, each pair a line that works on its own
 * @param bufferCapacity FE the buffer of the machine holds
 * @param maxInsert      FE the buffer accepts in one insert
 * @param speedPercent   how fast the machine works without Speed Upgrades, in percent of the time of a recipe
 */
public record MachineTier(int rank, int linePairs, long bufferCapacity, long maxInsert, int speedPercent) {

    public static final MachineTier BASIC = new MachineTier(1, 1, 8_000, 800, 100);
    public static final MachineTier ADVANCED = new MachineTier(2, 3, 32_000, 3_200, 175);
    public static final MachineTier SUPERIOR = new MachineTier(3, 5, 128_000, 12_800, 300);
    public static final MachineTier QUANTUM = new MachineTier(4, 7, 512_000, 51_200, 500);

    private static final List<MachineTier> ALL = List.of(BASIC, ADVANCED, SUPERIOR, QUANTUM);

    public MachineTier {
        requirePositive(rank, "rank");
        requirePositive(linePairs, "linePairs");
        requirePositive(bufferCapacity, "bufferCapacity");
        requirePositive(maxInsert, "maxInsert");
        requirePositive(speedPercent, "speedPercent");
    }

    /**
     * @param rank 1 to {@link #QUANTUM}'s rank
     * @throws IllegalArgumentException for a rank no tier has
     */
    public static MachineTier ofRank(final int rank) {
        if (rank < 1 || rank > ALL.size()) {
            throw new IllegalArgumentException("no machine tier of rank " + rank);
        }
        return ALL.get(rank - 1);
    }

    /**
     * @return the tier a machine of this one is upgraded to, empty for the last
     */
    public Optional<MachineTier> next() {
        return rank < ALL.size() ? Optional.of(ALL.get(rank)) : Optional.empty();
    }

    private static void requirePositive(final long value, final String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " of a tier must be positive: " + value);
        }
    }
}
