package com.morphengine.nexus.api.transport;

/**
 * How a device that moves resources reacts to a redstone signal.
 */
public enum RedstoneMode {

    /** Works whatever the signal. */
    IGNORED,

    /** Works only while it receives a signal. */
    HIGH_SIGNAL,

    /** Works only while it receives no signal. */
    LOW_SIGNAL,

    /** Does one operation each time a signal starts. */
    PULSE
}
