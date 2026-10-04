package com.morphengine.nexus.access;

import net.minecraft.world.entity.player.Player;

/**
 * A block entity that wants to know which player placed its block, as a
 * device does to work for them. Told once, as the block is placed. Server side.
 */
public interface PlayerPlaced {

    /**
     * @param player who placed the block, possibly a fake player of a mod standing in for one
     */
    void placedBy(Player player);
}
