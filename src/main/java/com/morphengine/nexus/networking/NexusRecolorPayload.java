package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NexusRecolorPayload(BlockPos pos, int rgb) implements CustomPacketPayload {

    public static final Type<NexusRecolorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus_recolor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NexusRecolorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NexusRecolorPayload::pos,
            ByteBufCodecs.VAR_INT, NexusRecolorPayload::rgb,
            NexusRecolorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
