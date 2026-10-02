package com.morphengine.nexus.transfer;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a Placer or Remover works with in the space its face touches, when it
 * handles items: blocks, or items lying loose. Fluids are always placed and
 * picked up as source blocks.
 */
public enum WorldMode implements StringRepresentable {

    /** A Placer places blocks, a Remover breaks them and takes what they drop. */
    BLOCKS,

    /** A Placer drops items, a Remover picks up the items lying there. */
    ITEMS;

    public static final StringRepresentable.EnumCodec<WorldMode> CODEC =
            StringRepresentable.fromEnum(WorldMode::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
