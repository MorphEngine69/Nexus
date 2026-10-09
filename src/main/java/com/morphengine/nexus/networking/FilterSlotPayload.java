package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

/**
 * Client to server: the player set a slot of the filter in the panel they look at.
 *
 * @param slot     the filter slot
 * @param resource what to list in it; {@code null} empties the slot
 */
public record FilterSlotPayload(int containerId, int slot, @Nullable NexusResource resource)
        implements CustomPacketPayload {

    public static final Type<FilterSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "filter_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FilterSlotPayload::containerId,
            ByteBufCodecs.VAR_INT, FilterSlotPayload::slot,
            NullableStreamCodec.of(NexusResources.STREAM_CODEC), FilterSlotPayload::resource,
            FilterSlotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
