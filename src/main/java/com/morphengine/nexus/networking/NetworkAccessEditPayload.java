package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.security.SecurityEdit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a change to the access of the network of the Nexus at
 * {@code pos}, from its panel. The server decides whether the player may make
 * it, and takes player names only from what it knows itself.
 */
public record NetworkAccessEditPayload(BlockPos pos, SecurityEdit edit) implements CustomPacketPayload {

    public static final Type<NetworkAccessEditPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "network_access_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkAccessEditPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NetworkAccessEditPayload::pos,
                    AccessCodecs.EDIT, NetworkAccessEditPayload::edit,
                    NetworkAccessEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
