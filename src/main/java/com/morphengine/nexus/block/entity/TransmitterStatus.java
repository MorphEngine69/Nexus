package com.morphengine.nexus.block.entity;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a Network Transmitter's panel says of its link.
 */
public enum TransmitterStatus implements StringRepresentable {

    /** It holds no linked Network Card. */
    NO_CARD,

    /** Its card is linked to a receiver that stands and is loaded; the network goes through. */
    LINKED,

    /** The receiver its card is linked to is gone, or its chunk or dimension is not loaded. */
    UNREACHABLE;

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
