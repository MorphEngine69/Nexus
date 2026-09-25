package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.network.Network;

/**
 * A device the player renames from the title of its panel. Server side only.
 */
public interface Renamable {

    int MAX_NAME_LENGTH = Network.MAX_NAME_LENGTH;

    /**
     * @param name the new name; surrounding spaces are dropped, and a blank name
     *             restores the device's default name
     * @throws IllegalArgumentException if the name is longer than {@link #MAX_NAME_LENGTH}
     */
    void rename(String name);
}
