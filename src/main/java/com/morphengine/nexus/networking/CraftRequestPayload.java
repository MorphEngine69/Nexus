package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.terminal.CraftRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Client to server: the player at a terminal asks to plan, or to start,
 * crafting {@code amount} of {@code resource}.
 */
public record CraftRequestPayload(int containerId, NexusResource resource, long amount, CraftRequest request)
        implements CustomPacketPayload {

    public static final Type<CraftRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "craft_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CraftRequestPayload::containerId,
                    NexusResources.STREAM_CODEC, CraftRequestPayload::resource,
                    ByteBufCodecs.VAR_LONG, CraftRequestPayload::amount,
                    NeoForgeStreamCodecs.enumCodec(CraftRequest.class), CraftRequestPayload::request,
                    CraftRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
