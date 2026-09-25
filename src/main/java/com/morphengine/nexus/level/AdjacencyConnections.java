package com.morphengine.nexus.level;

import com.morphengine.nexus.api.network.ConnectionProvider;
import com.morphengine.nexus.api.network.NetworkNode;
import com.morphengine.nexus.block.NetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Connections between network blocks touching each other in one level. Never
 * loads chunks: a neighbour in an unloaded chunk is skipped and remembered, so
 * the caller knows the traversal saw only part of the network.
 *
 * <p>Created per traversal on the server thread; not thread-safe.
 */
public final class AdjacencyConnections implements ConnectionProvider {

    private final Level level;
    private boolean reachedUnloaded;

    public AdjacencyConnections(final Level level) {
        this.level = level;
    }

    @Override
    public List<NetworkNode> connectionsOf(final NetworkNode node) {
        if (!(node instanceof BlockNode blockNode) || !blockNode.position().dimension().equals(level.dimension())) {
            return List.of();
        }
        final BlockPos pos = blockNode.position().pos();
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkBlock self)) {
            return List.of();
        }
        final List<NetworkNode> neighbours = new ArrayList<>(Direction.values().length);
        for (Direction side : Direction.values()) {
            final BlockPos neighbourPos = pos.relative(side);
            if (!level.isLoaded(neighbourPos)) {
                reachedUnloaded = true;
                continue;
            }
            final BlockState neighbour = level.getBlockState(neighbourPos);
            if (self.joins(state, side, neighbour)) {
                neighbours.add(BlockNode.of(level, neighbourPos));
            }
        }
        return neighbours;
    }

    /**
     * @return whether a traversal using this provider stopped at an unloaded chunk
     */
    public boolean reachedUnloaded() {
        return reachedUnloaded;
    }
}
