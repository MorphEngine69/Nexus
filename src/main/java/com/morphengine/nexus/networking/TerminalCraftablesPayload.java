package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: every resource the network of the terminal the player
 * looks at can craft, sent whenever the network's blueprints change.
 */
public record TerminalCraftablesPayload(int containerId, List<NexusResource> craftables)
        implements CustomPacketPayload {

    public static final Type<TerminalCraftablesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "terminal_craftables"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalCraftablesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalCraftablesPayload::containerId,
                    NexusResources.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalCraftablesPayload::craftables,
                    TerminalCraftablesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
