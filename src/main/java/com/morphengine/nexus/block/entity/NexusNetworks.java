package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.api.network.NetworkColor;

import java.util.UUID;

/**
 * What a Nexus's network is called and colored until its player says otherwise.
 */
public final class NexusNetworks {

    static final String DEFAULT_NAME = "Noticed Nexus";

    private NexusNetworks() {
    }

    /**
     * @return a network of its own, with the default name and color
     */
    static Network createDefault() {
        return new Network(UUID.randomUUID(), DEFAULT_NAME, NetworkColor.DEFAULT);
    }

    /**
     * @return whether the network has a name or a color of its own, which a
     *         Nexus broken in creative mode would otherwise lose
     */
    public static boolean isCustomized(final Network network) {
        return !network.name().equals(DEFAULT_NAME) || !network.color().equals(NetworkColor.DEFAULT);
    }
}
