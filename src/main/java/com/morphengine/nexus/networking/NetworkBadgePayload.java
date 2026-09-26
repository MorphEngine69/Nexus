package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Server to client: the network the device of the menu with the given container
 * id belongs to; {@code null} when it is in none.
 */
public record NetworkBadgePayload(int containerId, @Nullable NetworkBadge badge) implements CustomPacketPayload {

    public static final Type<NetworkBadgePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "network_badge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkBadgePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NetworkBadgePayload::containerId,
                    NullableStreamCodec.of(NetworkBadgeCodec.BADGE), NetworkBadgePayload::badge,
                    NetworkBadgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
