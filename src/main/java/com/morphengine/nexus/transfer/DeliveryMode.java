package com.morphengine.nexus.transfer;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * How much a Pusher delivers of each resource in its filter.
 */
public enum DeliveryMode implements StringRepresentable {

    /** As long as the storage beside it takes more. */
    UNLIMITED,

    /** Only until the storage beside it holds the amount set for the filter slot. */
    KEEP_STOCKED;

    public static final StringRepresentable.EnumCodec<DeliveryMode> CODEC =
            StringRepresentable.fromEnum(DeliveryMode::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
