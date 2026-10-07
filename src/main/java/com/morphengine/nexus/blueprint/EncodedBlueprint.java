package com.morphengine.nexus.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * A blueprint as encoded on a Blueprint item: a crafting recipe laid out on a
 * grid, or processing with the inputs and outputs listed.
 */
public sealed interface EncodedBlueprint permits CraftingBlueprint, ProcessingBlueprint {

    Codec<EncodedBlueprint> CODEC = BlueprintCodecs.KIND_CODEC.dispatch("kind", EncodedBlueprint::kind,
            EncodedBlueprint::mapCodecOf);

    StreamCodec<RegistryFriendlyByteBuf, EncodedBlueprint> STREAM_CODEC =
            NeoForgeStreamCodecs.enumCodec(BlueprintKind.class).<RegistryFriendlyByteBuf>cast()
                    .dispatch(EncodedBlueprint::kind, EncodedBlueprint::streamCodecOf);

    BlueprintKind kind();

    /**
     * @return whether the blueprint also takes substitutes of what it was encoded with
     */
    Substitution substitution();

    /**
     * @return what the network plans and runs with
     */
    Blueprint blueprint();

    private static MapCodec<? extends EncodedBlueprint> mapCodecOf(final BlueprintKind kind) {
        return switch (kind) {
            case CRAFTING -> CraftingBlueprint.MAP_CODEC;
            case PROCESSING -> ProcessingBlueprint.MAP_CODEC;
        };
    }

    private static StreamCodec<RegistryFriendlyByteBuf, ? extends EncodedBlueprint> streamCodecOf(
            final BlueprintKind kind) {
        return switch (kind) {
            case CRAFTING -> CraftingBlueprint.STREAM_CODEC;
            case PROCESSING -> ProcessingBlueprint.STREAM_CODEC;
        };
    }
}
