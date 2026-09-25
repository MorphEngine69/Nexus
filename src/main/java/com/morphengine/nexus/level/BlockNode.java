package com.morphengine.nexus.level;

import com.morphengine.nexus.api.network.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

/**
 * A network block identified by dimension and position, so that a graph can span
 * dimensions once non-adjacent connections exist.
 */
public record BlockNode(GlobalPos position) implements NetworkNode {

    public static BlockNode of(final Level level, final BlockPos pos) {
        return new BlockNode(GlobalPos.of(level.dimension(), pos.immutable()));
    }
}
