package com.morphengine.nexus.network;

import com.morphengine.nexus.api.network.ConnectionProvider;
import com.morphengine.nexus.api.network.NetworkNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Traversal over {@link NetworkNode}s. Edges come from a {@link ConnectionProvider},
 * so a node reachable through block adjacency and one reachable through a
 * transmitter/receiver pair are traversed the same way.
 */
public final class NetworkGraphs {

    private NetworkGraphs() {
    }

    /**
     * @return {@code start} and every node reachable from it, in breadth-first order
     */
    public static Set<NetworkNode> reachableFrom(final NetworkNode start, final ConnectionProvider connections) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(connections, "connections must not be null");

        final Set<NetworkNode> visited = new LinkedHashSet<>();
        final Deque<NetworkNode> pending = new ArrayDeque<>();
        visited.add(start);
        pending.add(start);

        while (!pending.isEmpty()) {
            final NetworkNode current = pending.poll();
            for (NetworkNode neighbor : connections.connectionsOf(current)) {
                if (visited.add(neighbor)) {
                    pending.add(neighbor);
                }
            }
        }

        return Set.copyOf(visited);
    }
}
