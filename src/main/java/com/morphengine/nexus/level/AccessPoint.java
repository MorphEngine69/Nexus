package com.morphengine.nexus.level;

import net.minecraft.core.GlobalPos;

/**
 * A network member through which a Nexus Terminal reaches the network from
 * afar, such as a Nexus Link. Server thread only.
 */
public interface AccessPoint extends NetworkMember {

    /**
     * @return where the access point stands
     */
    GlobalPos position();

    /**
     * @return how far it reaches, in blocks, in its own dimension only
     */
    int range();
}
