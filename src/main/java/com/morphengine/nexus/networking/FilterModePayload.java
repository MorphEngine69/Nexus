package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the player switched the filter in the panel they look at
 * between whitelist and blacklist.
 */
public record FilterModePayload(int containerId) implements CustomPacketPayload {

    public static final Type<FilterModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "filter_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterModePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<FilterModePayload>map(FilterModePayload::new, FilterModePayload::containerId).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
