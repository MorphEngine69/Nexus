package com.morphengine.nexus.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Size of an Energy Cell. A new tier is a new instance registered with its own
 * block; no other code changes.
 *
 * @param capacity    RF the cell holds, must be positive
 * @param maxTransfer RF the cell accepts or gives per operation, must be positive
 */
public record EnergyCellTier(long capacity, long maxTransfer) {

    /**
     * Placeholder balance until the numbers are settled.
     */
    public static final EnergyCellTier BASIC = new EnergyCellTier(100_000, 1_000);

    public static final Codec<EnergyCellTier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("capacity").forGetter(EnergyCellTier::capacity),
                    Codec.LONG.fieldOf("max_transfer").forGetter(EnergyCellTier::maxTransfer))
            .apply(instance, EnergyCellTier::new));

    public EnergyCellTier {
        if (capacity <= 0 || maxTransfer <= 0) {
            throw new IllegalArgumentException(
                    "energy cell tier needs positive values: capacity=" + capacity + ", maxTransfer=" + maxTransfer);
        }
    }
}
