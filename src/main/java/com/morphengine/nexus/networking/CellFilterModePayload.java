package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the player switched the filter of the Vault Cell they hold
 * between whitelist and blacklist.
 */
public record CellFilterModePayload(int containerId) implements CustomPacketPayload {

    public static final Type<CellFilterModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "cell_filter_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CellFilterModePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<CellFilterModePayload>map(
                    CellFilterModePayload::new, CellFilterModePayload::containerId).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
