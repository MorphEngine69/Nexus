package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server to client: the draft of the Blueprint Terminal whose panel the player
 * looks at, and what its crafting grid crafts.
 *
 * @param craftingOutputs the result and what the ingredients leave behind; empty
 *                        for a processing draft or a grid that crafts nothing
 */
public record BlueprintDraftPayload(int containerId, BlueprintDraft draft, List<ResourceAmount> craftingOutputs)
        implements CustomPacketPayload {

    public static final Type<BlueprintDraftPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "blueprint_draft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintDraftPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BlueprintDraftPayload::containerId,
                    BlueprintDraft.STREAM_CODEC, BlueprintDraftPayload::draft,
                    NexusResources.AMOUNT_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    BlueprintDraftPayload::craftingOutputs,
                    BlueprintDraftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
