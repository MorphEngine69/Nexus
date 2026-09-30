package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the player changed how much a processing slot of the
 * Blueprint Terminal's draft lists.
 *
 * @param amount the new amount; the server clamps it
 */
public record BlueprintAmountPayload(int containerId, int slot, long amount) implements CustomPacketPayload {

    public static final Type<BlueprintAmountPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "blueprint_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintAmountPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BlueprintAmountPayload::containerId,
                    ByteBufCodecs.VAR_INT, BlueprintAmountPayload::slot,
                    ByteBufCodecs.VAR_LONG, BlueprintAmountPayload::amount,
                    BlueprintAmountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
