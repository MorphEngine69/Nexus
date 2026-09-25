package com.morphengine.nexus.api.network;

import java.util.Collection;

/**
 * Source of the edges of a network graph. Block adjacency is one kind of
 * connection; a linked transmitter and receiver, possibly in another dimension,
 * is another. Graph traversal only sees this interface, so new kinds of
 * connection are added without touching the traversal.
 */
@FunctionalInterface
public interface ConnectionProvider {

    /**
     * @return nodes directly connected to {@code node}; empty when it has none or
     *         is not a node this provider knows
     */
    Collection<? extends NetworkNode> connectionsOf(NetworkNode node);
}
