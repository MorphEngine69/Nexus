package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.terminal.CraftRequest;
import com.morphengine.nexus.terminal.PlanPreview;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Server to client: the answer to a {@link CraftRequestPayload}, the plan as
 * worked out now.
 *
 * @param outcome {@link CraftRequest#START} when the task started, {@link
 *                CraftRequest#CRAFT_LESS} when the plan is for the most that can
 *                start, {@link CraftRequest#PREVIEW} when it was only planned or
 *                could not start
 */
public record CraftPlanPayload(int containerId, PlanPreview plan, CraftRequest outcome)
        implements CustomPacketPayload {

    public static final Type<CraftPlanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "craft_plan"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPlanPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CraftPlanPayload::containerId,
                    PlanPreview.STREAM_CODEC, CraftPlanPayload::plan,
                    NeoForgeStreamCodecs.enumCodec(CraftRequest.class), CraftPlanPayload::outcome,
                    CraftPlanPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
