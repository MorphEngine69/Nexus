package com.morphengine.nexus.blueprint;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Locale;

/**
 * Whether a Blueprint takes nothing but the resources it was encoded with, or
 * also their substitutes: for crafting, whatever the recipe accepts in a slot;
 * for processing, whatever is in the tag picked for an input.
 */
public enum Substitution implements StringRepresentable {

    EXACT,
    ALLOWED;

    public static final Codec<Substitution> CODEC = StringRepresentable.fromEnum(Substitution::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, Substitution> STREAM_CODEC =
            NeoForgeStreamCodecs.enumCodec(Substitution.class);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    public boolean isAllowed() {
        return this == ALLOWED;
    }

    public Substitution toggled() {
        return isAllowed() ? EXACT : ALLOWED;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
