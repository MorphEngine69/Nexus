package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Client to server: the player at a Crafting Monitor cancels a crafting task.
 */
public record CancelTaskPayload(int containerId, UUID task) implements CustomPacketPayload {

    public static final Type<CancelTaskPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "cancel_task"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelTaskPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CancelTaskPayload::containerId,
                    UUIDUtil.STREAM_CODEC, CancelTaskPayload::task,
                    CancelTaskPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
