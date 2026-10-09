package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the player typed a new name into the title of the panel of
 * the device at {@code pos}.
 */
public record DeviceRenamePayload(BlockPos pos, String name) implements CustomPacketPayload {

    public static final Type<DeviceRenamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "device_rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeviceRenamePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DeviceRenamePayload::pos,
            ByteBufCodecs.STRING_UTF8, DeviceRenamePayload::name,
            DeviceRenamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
