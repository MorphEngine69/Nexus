package com.morphengine.nexus.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The devices that carry a network beyond its cables.
 */
public enum WirelessKind implements StringRepresentable {

    /**
     * Carries its network to the Network Receiver its Network Card is linked
     * to, however far, in whatever dimension.
     */
    TRANSMITTER(true),

    /** Where a Network Transmitter's network comes out; blocks around it join that network. */
    RECEIVER(true),

    /** Lets Nexus Terminals within its range reach its network. */
    LINK(false);

    public static final Codec<WirelessKind> CODEC = StringRepresentable.fromEnum(WirelessKind::values);

    private final boolean linkEnd;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    WirelessKind(final boolean linkEnd) {
        this.linkEnd = linkEnd;
    }

    /**
     * @return whether the device is one end of a link between a transmitter and a receiver
     */
    public boolean isLinkEnd() {
        return linkEnd;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
