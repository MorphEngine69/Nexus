package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Client to server: the player set a filter slot of the Vault Cell they hold.
 *
 * @param slot     the filter slot
 * @param resource what to list in it; {@code null} empties the slot
 */
public record CellFilterPayload(int containerId, int slot, @Nullable NexusResource resource)
        implements CustomPacketPayload {

    public static final Type<CellFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "cell_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CellFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CellFilterPayload::containerId,
            ByteBufCodecs.VAR_INT, CellFilterPayload::slot,
            NullableStreamCodec.of(NexusResources.STREAM_CODEC), CellFilterPayload::resource,
            CellFilterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
