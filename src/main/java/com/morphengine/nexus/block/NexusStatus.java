package com.morphengine.nexus.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a Nexus shows on its model, so the state of the network reads from a
 * glance at the block.
 */
public enum NexusStatus implements StringRepresentable {

    /** The network has energy: the core glows and the cables carry light. */
    ONLINE,

    /** The network's energy pool is empty: the core is dark. */
    NO_ENERGY,

    /**
     * Another Nexus leads this network; this one runs nothing and flashes until
     * it is removed or cut off.
     */
    CONFLICT;

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
