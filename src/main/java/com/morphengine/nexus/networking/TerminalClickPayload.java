package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.terminal.GridClick;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jspecify.annotations.Nullable;

/**
 * Client to server: the player clicked the grid of the terminal menu with the
 * given container id.
 *
 * @param resource the resource clicked; {@code null} for an empty spot of the grid
 */
public record TerminalClickPayload(int containerId, @Nullable NexusResource resource, GridClick click)
        implements CustomPacketPayload {

    public static final Type<TerminalClickPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "terminal_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalClickPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalClickPayload::containerId,
                    NullableStreamCodec.of(NexusResources.STREAM_CODEC), TerminalClickPayload::resource,
                    NeoForgeStreamCodecs.enumCodec(GridClick.class), TerminalClickPayload::click,
                    TerminalClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
