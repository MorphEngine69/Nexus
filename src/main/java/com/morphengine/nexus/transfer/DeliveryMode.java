package com.morphengine.nexus.transfer;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * How much of each resource its whitelist lists a Puller or Pusher moves. Keeping
 * stock takes a Regulator Upgrade; without one a device moves without limit,
 * whatever it is set to.
 */
public enum DeliveryMode implements StringRepresentable {

    /** A Pusher as long as the storage beside it takes more, a Puller as long as it holds any. */
    UNLIMITED,

    /**
     * A Pusher only until the storage beside it holds the amount set for the
     * filter slot, a Puller only what the storage holds beyond that amount.
     */
    KEEP_STOCKED;

    public static final StringRepresentable.EnumCodec<DeliveryMode> CODEC =
            StringRepresentable.fromEnum(DeliveryMode::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
