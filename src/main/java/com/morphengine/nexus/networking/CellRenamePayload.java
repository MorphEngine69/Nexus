package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the player typed a new name into the title of the panel of
 * the Vault Cell they hold, in the menu with the given container id.
 */
public record CellRenamePayload(int containerId, String name) implements CustomPacketPayload {

    public static final Type<CellRenamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "cell_rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CellRenamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CellRenamePayload::containerId,
            ByteBufCodecs.STRING_UTF8, CellRenamePayload::name,
            CellRenamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
