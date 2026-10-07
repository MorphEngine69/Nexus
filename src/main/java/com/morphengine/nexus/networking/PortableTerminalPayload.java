package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Client to server: the player pressed a key of the Nexus Terminal they carry, to open it or to switch its mode.
 */
public record PortableTerminalPayload(Action action) implements CustomPacketPayload {

    /**
     * What the key asks of the terminal.
     */
    public enum Action {
        OPEN,
        SWITCH_MODE
    }

    public static final Type<PortableTerminalPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "portable_terminal"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Action> ACTION =
            NeoForgeStreamCodecs.enumCodec(Action.class);

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableTerminalPayload> STREAM_CODEC =
            ACTION.map(PortableTerminalPayload::new, PortableTerminalPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
