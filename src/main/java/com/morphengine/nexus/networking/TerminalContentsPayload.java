package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.terminal.TerminalEntry;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.List;

/**
 * Server to client: what changed in the storage shown by the terminal menu with
 * the given container id.
 *
 * @param reset   forget everything known before applying {@code entries}; the
 *                first part of a full listing
 * @param entries new amounts; an amount of zero removes the resource
 */
public record TerminalContentsPayload(int containerId, TerminalStatus status, boolean reset,
                                      List<TerminalEntry> entries) implements CustomPacketPayload {

    public static final Type<TerminalContentsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "terminal_contents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalContentsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalContentsPayload::containerId,
                    NeoForgeStreamCodecs.enumCodec(TerminalStatus.class), TerminalContentsPayload::status,
                    ByteBufCodecs.BOOL, TerminalContentsPayload::reset,
                    TerminalEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalContentsPayload::entries,
                    TerminalContentsPayload::new);

    public TerminalContentsPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
