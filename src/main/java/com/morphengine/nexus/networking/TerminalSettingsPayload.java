package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the player changed how the terminal of the menu with the
 * given container id lists the storage.
 */
public record TerminalSettingsPayload(int containerId, TerminalSettings settings) implements CustomPacketPayload {

    public static final Type<TerminalSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "terminal_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalSettingsPayload::containerId,
                    TerminalSettings.STREAM_CODEC, TerminalSettingsPayload::settings,
                    TerminalSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
