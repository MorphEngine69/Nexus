package com.morphengine.nexus.block.entity;

import net.minecraft.world.MenuProvider;

/**
 * A block entity whose device opens a menu on right click.
 */
public interface MenuHost extends MenuProvider {

    /**
     * Called by the menu when the player closes it.
     */
    void markClosed();

    /**
     * Called by the block when a player places it.
     */
    void markPlaced();

    /**
     * @return whether a click now is the held right button repeating after the
     *         device was placed or its menu closed, not a new request to open it
     */
    boolean ignoresClick();
}
