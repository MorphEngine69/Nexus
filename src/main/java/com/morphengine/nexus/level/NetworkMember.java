package com.morphengine.nexus.level;

/**
 * A block entity that wants to know which network it belongs to. The network's
 * controller tells it when a rebuild adds it to or drops it from the network.
 * Server thread only.
 */
public interface NetworkMember {

    void joinNetwork(NetworkController controller);

    /**
     * Only takes effect if {@code controller} is the one this member last joined.
     */
    void leaveNetwork(NetworkController controller);
}
