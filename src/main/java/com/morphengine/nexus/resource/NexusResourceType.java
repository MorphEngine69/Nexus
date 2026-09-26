package com.morphengine.nexus.resource;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.api.resource.ResourceType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/**
 * A registered kind of resource with how the game saves, sends and shows its
 * keys. A new kind, such as a gas from another mod, is a new registered instance.
 *
 * @param <R> the keys of this kind
 */
public final class NexusResourceType<R extends NexusResource> implements ResourceType {

    private final MapCodec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;
    private final AmountUnit unit;

    public NexusResourceType(
            final MapCodec<R> codec, final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec, final AmountUnit unit) {
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        this.streamCodec = Objects.requireNonNull(streamCodec, "streamCodec must not be null");
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
    }

    public MapCodec<R> codec() {
        return codec;
    }

    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }

    public AmountUnit unit() {
        return unit;
    }
}
