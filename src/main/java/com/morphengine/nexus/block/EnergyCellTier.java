package com.morphengine.nexus.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Size of an Energy Cell. A new tier is a new instance registered with its own
 * block; no other code changes.
 *
 * @param rank        the place of the tier in the line, from 1; the cell shows as many marks
 * @param capacity    FE the cell holds, must be positive
 * @param maxTransfer FE the cell accepts or gives per operation, must be positive
 */
public record EnergyCellTier(int rank, long capacity, long maxTransfer) {

    /**
     * Placeholder balance until the numbers are settled: every tier holds and moves ten times what the one below
     * does.
     */
    public static final EnergyCellTier BASIC = new EnergyCellTier(1, 50_000, 500);
    public static final EnergyCellTier ADVANCED = new EnergyCellTier(2, 500_000, 5_000);
    public static final EnergyCellTier SUPERIOR = new EnergyCellTier(3, 5_000_000, 50_000);
    public static final EnergyCellTier QUANTUM = new EnergyCellTier(4, 50_000_000, 500_000);

    public static final Codec<EnergyCellTier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("rank").forGetter(EnergyCellTier::rank),
                    Codec.LONG.fieldOf("capacity").forGetter(EnergyCellTier::capacity),
                    Codec.LONG.fieldOf("max_transfer").forGetter(EnergyCellTier::maxTransfer))
            .apply(instance, EnergyCellTier::new));

    public EnergyCellTier {
        if (rank <= 0 || capacity <= 0 || maxTransfer <= 0) {
            throw new IllegalArgumentException("energy cell tier needs positive values: rank=" + rank
                    + ", capacity=" + capacity + ", maxTransfer=" + maxTransfer);
        }
    }
}
