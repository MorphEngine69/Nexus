package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.transfer.TransferSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the settings of the Puller or Pusher whose panel the player
 * looks at changed.
 */
public record TransferSettingsPayload(int containerId, TransferSettings settings) implements CustomPacketPayload {

    public static final Type<TransferSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "transfer_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TransferSettingsPayload::containerId,
                    TransferSettings.STREAM_CODEC, TransferSettingsPayload::settings,
                    TransferSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
