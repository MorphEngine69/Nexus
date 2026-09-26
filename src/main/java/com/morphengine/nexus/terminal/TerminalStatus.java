package com.morphengine.nexus.terminal;

/**
 * Whether a terminal can reach its network's storage.
 */
public enum TerminalStatus {

    ONLINE,

    /** The network's energy pool is empty; the storage is out of reach until it charges. */
    NO_ENERGY,

    /** No Nexus is connected. */
    NO_NETWORK
}
