package com.morphengine.nexus.metal;

import net.minecraft.util.valueproviders.IntProvider;

import java.util.Objects;

/**
 * How the blocks of a metal break.
 *
 * @param ore        the ore
 * @param storage    the block of the metal
 * @param experience experience the ore drops
 */
public record MetalBlocks(BlockFeel ore, BlockFeel storage, IntProvider experience) {

    public MetalBlocks {
        Objects.requireNonNull(ore, "ore");
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(experience, "experience");
    }
}
