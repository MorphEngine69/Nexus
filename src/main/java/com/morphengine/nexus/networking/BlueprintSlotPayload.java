package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Client to server: the player set a ghost slot of the Blueprint Terminal's draft.
 *
 * @param slot     the draft slot
 * @param resource what to list in it; {@code null} empties the slot
 */
public record BlueprintSlotPayload(int containerId, int slot, @Nullable NexusResource resource)
        implements CustomPacketPayload {

    public static final Type<BlueprintSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "blueprint_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintSlotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BlueprintSlotPayload::containerId,
                    ByteBufCodecs.VAR_INT, BlueprintSlotPayload::slot,
                    NullableStreamCodec.of(NexusResources.STREAM_CODEC), BlueprintSlotPayload::resource,
                    BlueprintSlotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
