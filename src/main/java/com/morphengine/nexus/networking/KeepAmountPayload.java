package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the player set how much of the resource in a filter slot
 * the Pusher they look at keeps stocked.
 *
 * @param amount units to keep; the server clamps it
 */
public record KeepAmountPayload(int containerId, int slot, long amount) implements CustomPacketPayload {

    public static final Type<KeepAmountPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "keep_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeepAmountPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KeepAmountPayload::containerId,
            ByteBufCodecs.VAR_INT, KeepAmountPayload::slot,
            ByteBufCodecs.VAR_LONG, KeepAmountPayload::amount,
            KeepAmountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
