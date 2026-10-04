package com.morphengine.nexus.api.network.security;

/**
 * Something a player may or may not do with a network. Every check names
 * exactly one permission; what a player holds comes from their {@link Role},
 * adjusted one permission at a time where the role allows it.
 */
public enum Permission {

    /** Open the panels of the network's devices and its terminals, and see what the network holds. */
    OPEN,

    /**
     * Put resources into the network: at a terminal, or through a device of
     * the player that takes them in, such as a Puller or a Remover.
     */
    INSERT,

    /**
     * Take resources out of the network: at a terminal, or through a device of
     * the player that gives them out, such as a Pusher or a Placer.
     */
    EXTRACT,

    /** Order crafting, by hand or through a device of the player, and cancel crafting tasks. */
    AUTOCRAFTING,

    /**
     * Change the settings of the network and its devices, and what their slots
     * hold: names, colors, filters, modes, priorities, upgrades, cells,
     * Blueprints, network cards.
     */
    CONFIGURE,

    /** Place and break the blocks of the network, and connect other network blocks to it. */
    BUILD,

    /**
     * Decide who has access to the network. Only the owner and admins hold it;
     * it comes with the role alone and is never {@linkplain #isAdjustable adjusted}.
     */
    MANAGE;

    /**
     * @return whether a single player may be granted or denied this permission
     *         apart from their role
     */
    public boolean isAdjustable() {
        return this != MANAGE;
    }
}
