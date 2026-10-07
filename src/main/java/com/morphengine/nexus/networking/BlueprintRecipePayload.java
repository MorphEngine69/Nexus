package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a recipe viewer lays a recipe out as the draft of the
 * Blueprint Terminal the player looks at.
 */
public record BlueprintRecipePayload(int containerId, BlueprintDraft draft) implements CustomPacketPayload {

    public static final Type<BlueprintRecipePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "blueprint_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintRecipePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BlueprintRecipePayload::containerId,
                    BlueprintDraft.STREAM_CODEC, BlueprintRecipePayload::draft,
                    BlueprintRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
