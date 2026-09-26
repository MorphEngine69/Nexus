package com.morphengine.nexus.terminal;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a terminal sorts its resources by.
 */
public enum SortOrder implements StringRepresentable {

    /** How much of the resource the network holds. */
    AMOUNT,

    /** The resource's name as the player reads it. */
    NAME,

    /** The mod that adds the resource, then its name. */
    MOD,

    /** The registry id of the resource. */
    ID;

    public static final StringRepresentable.EnumCodec<SortOrder> CODEC =
            StringRepresentable.fromEnum(SortOrder::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
