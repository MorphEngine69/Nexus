package com.morphengine.nexus.block;

import com.morphengine.nexus.api.network.Paint;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that joins a network by touching another network block, directly or
 * through cables.
 */
public interface NetworkBlock {

    Paint paint();

    /**
     * @return whether this block is a device that does work and is listed in the
     *         Nexus interface; cables only carry the network and return false
     */
    default boolean isDevice() {
        return true;
    }

    /**
     * @return whether this block, in {@code state}, takes a network connection on
     *         {@code side}; a machine refuses it on its working face
     */
    default boolean acceptsConnection(BlockState state, Direction side) {
        return true;
    }

    /**
     * Whether this block, in {@code state}, joins the {@code neighbour} beyond its
     * {@code side}. Symmetric: both blocks must take a connection on the face
     * they share, and their paints must match.
     */
    default boolean joins(BlockState state, Direction side, BlockState neighbour) {
        return neighbour.getBlock() instanceof NetworkBlock other
                && paint().connectsTo(other.paint())
                && acceptsConnection(state, side)
                && other.acceptsConnection(neighbour, side.getOpposite());
    }
}
