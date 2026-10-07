package com.morphengine.nexus.api.network;

/**
 * What a device does in its network, as counted in the Nexus interface.
 */
public enum DeviceRole {

    /** Lends storage to the network, such as a Storage Vault. */
    STORAGE,

    /** Takes resources from the world into the network. */
    PULLER,

    /** Delivers resources from the network into the world. */
    PUSHER,

    /** Processes resources, such as a furnace of the network. */
    MACHINE,

    /** Anything else: the Nexus, energy cells, generators, terminals. */
    OTHER
}
