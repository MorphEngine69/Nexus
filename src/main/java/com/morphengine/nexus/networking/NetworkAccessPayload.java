package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.AccessView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the access of the network, for the Nexus menu with the given container id.
 */
public record NetworkAccessPayload(int containerId, AccessView view) implements CustomPacketPayload {

    public static final Type<NetworkAccessPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "network_access"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkAccessPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NetworkAccessPayload::containerId,
                    AccessCodecs.VIEW, NetworkAccessPayload::view,
                    NetworkAccessPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
