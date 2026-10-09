package com.morphengine.nexus.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * How resources of any kind are saved and sent: each carries the id of its kind,
 * which picks the codec for the rest.
 */
public final class NexusResources {

    public static final Codec<NexusResource> CODEC = ResourceTypes.REGISTRY.byNameCodec()
            .dispatch("type", NexusResource::type, NexusResourceType::codec);

    private static final StreamCodec<RegistryFriendlyByteBuf, NexusResourceType<?>> TYPE_STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.<NexusResourceType<?>>map(NexusResources::typeOf, ResourceTypes::idOf).cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, NexusResource> STREAM_CODEC =
            TYPE_STREAM_CODEC.dispatch(NexusResource::type, NexusResourceType::streamCodec);

    public static final Codec<ResourceAmount> AMOUNT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    CODEC.fieldOf("resource").forGetter(amount -> of(amount.resource())),
                    Codec.LONG.fieldOf("amount").forGetter(ResourceAmount::amount))
            .apply(instance, ResourceAmount::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceAmount> AMOUNT_STREAM_CODEC =
            StreamCodec.composite(
                    STREAM_CODEC, amount -> of(amount.resource()),
                    ByteBufCodecs.VAR_LONG, ResourceAmount::amount,
                    ResourceAmount::new);

    private NexusResources() {
    }

    /**
     * @return {@code resource} as a resource of the game; every resource in a
     *         network is one, since only the mod creates them
     * @throws IllegalArgumentException if it is not
     */
    public static NexusResource of(final ResourceKey resource) {
        if (resource instanceof NexusResource nexusResource) {
            return nexusResource;
        }
        throw new IllegalArgumentException("not a resource of the game: " + resource);
    }

    /**
     * @return {@code resources} as resources of the game, in the same order
     * @throws IllegalArgumentException if one is not
     */
    public static List<NexusResource> listOf(final List<? extends ResourceKey> resources) {
        final List<NexusResource> list = new ArrayList<>(resources.size());
        for (ResourceKey resource : resources) {
            list.add(of(resource));
        }
        return List.copyOf(list);
    }

    private static NexusResourceType<?> typeOf(final ResourceLocation id) {
        return ResourceTypes.REGISTRY.getOptional(id)
                .orElseThrow(() -> new DecoderException("unknown resource type " + id));
    }
}
