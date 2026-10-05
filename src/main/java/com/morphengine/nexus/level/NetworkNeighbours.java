package com.morphengine.nexus.level;

import com.morphengine.nexus.block.NetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Who stands against a block of the mod: used to let only blocks of a network reach what a block gives, so that
 * everything that leaves a network goes through the devices that keep to its rules.
 */
public final class NetworkNeighbours {

    private NetworkNeighbours() {
    }

    /**
     * Does not load a chunk to find out.
     *
     * @param pos  the block whose neighbour is asked about
     * @param side the side of that block; {@code null} for a request that says no side
     * @return whether the block on {@code side} of {@code pos} is loaded and is a block of a network; {@code false}
     *         for a block of another mod, for air and when there is no side to look at
     */
    public static boolean hasNetworkBlockBeyond(final Level level, final BlockPos pos, final @Nullable Direction side) {
        if (side == null) {
            return false;
        }
        final BlockPos beyond = pos.relative(side);
        return level.isLoaded(beyond) && level.getBlockState(beyond).getBlock() instanceof NetworkBlock;
    }
}
