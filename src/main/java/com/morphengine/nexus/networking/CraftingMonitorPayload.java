package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.blueprint.BlueprintCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: every crafting task of the network of the Crafting Monitor
 * whose panel the player looks at.
 */
public record CraftingMonitorPayload(int containerId, List<TaskStatus> tasks) implements CustomPacketPayload {

    public static final Type<CraftingMonitorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "crafting_monitor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingMonitorPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CraftingMonitorPayload::containerId,
                    BlueprintCodecs.STATUS_STREAM_CODEC.apply(ByteBufCodecs.list()), CraftingMonitorPayload::tasks,
                    CraftingMonitorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
