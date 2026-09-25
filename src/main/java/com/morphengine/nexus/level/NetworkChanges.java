package com.morphengine.nexus.level;

import com.morphengine.nexus.api.network.NetworkNode;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.NetworkBlock;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.network.NetworkGraphs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Tells every {@link NetworkController} whose network is affected by a network block appearing or
 * disappearing to rebuild its membership, and returns devices no controller
 * reaches any more to the unconnected color, their cables dark. Runs only when
 * such a block changes, never on a timer.
 */
public final class NetworkChanges {

    private NetworkChanges() {
    }

    /**
     * Called from {@code onPlace}, which also fires for state changes of the same
     * block; only a block that was not there before affects the graph.
     */
    public static void blockPlaced(
            final Level level, final BlockPos pos, final BlockState state, final BlockState oldState) {
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            invalidateReachable(level, Set.of(pos));
        }
    }

    /**
     * Called after the block is gone, so each former neighbour is searched on its
     * own: removing a cable may have split one network into several.
     */
    public static void blockRemoved(final Level level, final BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        final Set<BlockPos> neighbours = new HashSet<>();
        for (Direction side : Direction.values()) {
            final BlockPos neighbour = pos.relative(side);
            if (level.isLoaded(neighbour) && level.getBlockState(neighbour).getBlock() instanceof NetworkBlock) {
                neighbours.add(neighbour);
            }
        }
        invalidateReachable(level, neighbours);
    }

    /**
     * A part left with a controller is repainted by its rebuild; a part left
     * without one shows the unconnected color right away, unless the search
     * stopped at an unloaded chunk where its controller may be.
     */
    private static void invalidateReachable(final Level level, final Set<BlockPos> starts) {
        final Set<NetworkNode> visited = new HashSet<>();
        for (BlockPos start : starts) {
            final BlockNode startNode = BlockNode.of(level, start);
            if (visited.contains(startNode)) {
                continue;
            }
            final AdjacencyConnections connections = new AdjacencyConnections(level);
            final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(startNode, connections);
            visited.addAll(reachable);
            if (!invalidateControllers(level, reachable) && !connections.reachedUnloaded()) {
                paintUnconnected(level, reachable);
            }
        }
    }

    private static boolean invalidateControllers(final Level level, final Set<NetworkNode> part) {
        boolean found = false;
        for (NetworkNode node : part) {
            if (node instanceof BlockNode blockNode
                    && level.getBlockEntity(blockNode.position().pos()) instanceof NetworkController controller) {
                controller.invalidateNetwork();
                found = true;
            }
        }
        return found;
    }

    private static void paintUnconnected(final Level level, final Set<NetworkNode> part) {
        for (NetworkNode node : part) {
            if (node instanceof BlockNode blockNode) {
                final BlockPos pos = blockNode.position().pos();
                NetworkColoring.paint(level, pos, NetworkColoring.UNCONNECTED);
                CableBlock.showPower(level, pos, false);
            }
        }
    }
}
