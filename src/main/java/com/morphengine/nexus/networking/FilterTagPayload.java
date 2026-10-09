package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the player moved a slot of the filter in the panel they
 * look at on to another tag of its resource.
 *
 * @param slot the filter slot
 * @param step one for the next tag, minus one for the previous
 */
public record FilterTagPayload(int containerId, int slot, int step) implements CustomPacketPayload {

    public static final Type<FilterTagPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "filter_tag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterTagPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FilterTagPayload::containerId,
            ByteBufCodecs.VAR_INT, FilterTagPayload::slot,
            ByteBufCodecs.VAR_INT, FilterTagPayload::step,
            FilterTagPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
