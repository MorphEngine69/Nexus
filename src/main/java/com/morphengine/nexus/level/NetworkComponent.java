package com.morphengine.nexus.level;

import java.util.List;

/**
 * One aspect of a network, such as its shared storage. Every network owns one
 * instance of each {@link NetworkComponentType}; a new aspect is a new type,
 * the network itself does not change. Server thread only.
 */
public interface NetworkComponent {

    /**
     * Takes over the members found by a rebuild of the network. Members missing
     * from {@code members} have left it; an empty list means the network is gone
     * or its Nexus stands down.
     */
    void adopt(List<NetworkMember> members);
}
