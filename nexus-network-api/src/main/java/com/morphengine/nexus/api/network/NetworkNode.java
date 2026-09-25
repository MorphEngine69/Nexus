package com.morphengine.nexus.api.network;

/**
 * A participant in a network graph. Identity and connections are supplied by the
 * caller of {@link NetworkGraphs#reachableFrom}, not by this type, so that graph
 * traversal never has to assume block adjacency as the only kind of connection.
 */
public interface NetworkNode {
}
