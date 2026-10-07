package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the energy tab of the Nexus menu with the given container id is shown or hidden, so that the
 * server sends its report only while it is looked at.
 */
public record NexusEnergyTabPayload(int containerId, boolean shown) implements CustomPacketPayload {

    public static final Type<NexusEnergyTabPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus_energy_tab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NexusEnergyTabPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NexusEnergyTabPayload::containerId,
                    ByteBufCodecs.BOOL, NexusEnergyTabPayload::shown,
                    NexusEnergyTabPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
