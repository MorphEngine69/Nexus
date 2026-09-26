package com.morphengine.nexus.terminal;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Whether a terminal lists its resources from the smallest to the largest or the other way.
 */
public enum SortDirection implements StringRepresentable {

    ASCENDING,
    DESCENDING;

    public static final StringRepresentable.EnumCodec<SortDirection> CODEC =
            StringRepresentable.fromEnum(SortDirection::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
