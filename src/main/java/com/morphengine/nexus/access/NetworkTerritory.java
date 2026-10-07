package com.morphengine.nexus.access;

import com.morphengine.nexus.api.network.NetworkNode;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.level.BlockNode;
import com.morphengine.nexus.level.ServerConnections;
import com.morphengine.nexus.network.NetworkGraphs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Whose rules a network block falls under: those of every network it is part
 * of or touches, found by following its connections, wireless links included,
 * as far as loaded chunks go. A network whose Nexus is unloaded is still found
 * through any of its devices, which remember the network they were in. Runs
 * only when a network block is placed or broken.
 */
final class NetworkTerritory {

    private NetworkTerritory() {
    }

    /**
     * @param excluded a block whose own rules are not asked, such as a Nexus that
     *                 has only just been placed and has no network yet; {@code null}
     *                 to ask every block
     * @return the rules of every network the blocks connected to {@code start}
     *         belong to, each once
     */
    static List<AccessPolicy> rulesAround(final ServerLevel level, final BlockPos start,
                                          final @Nullable GlobalPos excluded) {
        final ServerConnections connections = new ServerConnections(level.getServer());
        final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(BlockNode.of(level, start), connections);
        final Set<AccessPolicy> rules = Collections.newSetFromMap(new IdentityHashMap<>());
        for (NetworkNode node : reachable) {
            if (node instanceof BlockNode block && !block.position().equals(excluded)
                    && connections.levelOf(block) instanceof Level at
                    && at.getBlockEntity(block.position().pos()) instanceof Secured secured) {
                rules.add(secured.accessPolicy());
            }
        }
        return List.copyOf(rules);
    }
}
