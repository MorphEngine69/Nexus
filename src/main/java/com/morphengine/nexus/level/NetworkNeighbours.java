package com.morphengine.nexus.level;

import com.morphengine.nexus.block.NetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /**
     * Lets only a block of a network reach what a block entity offers: the energy of the Nexus goes in and out of the
     * network only through a Puller or a Pusher, which keep to its rules, so a block of another mod gets nothing.
     *
     * @param side    the side of the block entity that is asked about; {@code null} when the asker names no side
     * @param offered what the block entity gives a block of a network
     * @return {@code offered} when a block of a network stands beyond {@code side}, otherwise {@code null}
     */
    public static <T> @Nullable T offeredBeyond(
            final BlockEntity blockEntity, final @Nullable Direction side, final T offered) {
        final Level level = blockEntity.getLevel();
        return level != null && hasNetworkBlockBeyond(level, blockEntity.getBlockPos(), side) ? offered : null;
    }
}
