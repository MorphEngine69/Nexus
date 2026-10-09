package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.terminal.GridFill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.List;

/**
 * Client to server: a recipe viewer such as JEI, REI or EMI asks to lay out a
 * recipe on the grid of the Crafting Terminal menu with the given container id.
 *
 * @param slots for each of the nine grid slots, the items that fit it in order of preference
 */
public record CraftingGridFillPayload(int containerId, List<List<ItemKey>> slots, GridFill amount)
        implements CustomPacketPayload {

    public static final Type<CraftingGridFillPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "crafting_grid_fill"));

    public static final int GRID_SLOTS = 9;
    /** Enough for every item of a tag like planks or wool, and a bound against oversized packets. */
    public static final int MOST_OPTIONS = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingGridFillPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CraftingGridFillPayload::containerId,
                    ItemKey.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_OPTIONS)).apply(ByteBufCodecs.list(GRID_SLOTS)),
                    CraftingGridFillPayload::slots,
                    NeoForgeStreamCodecs.enumCodec(GridFill.class), CraftingGridFillPayload::amount,
                    CraftingGridFillPayload::new);

    public CraftingGridFillPayload {
        slots = slots.stream().map(List::copyOf).toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
