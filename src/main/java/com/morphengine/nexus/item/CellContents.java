package com.morphengine.nexus.item;

import com.mojang.serialization.Codec;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Objects;

/**
 * What a Vault Cell holds, kept on the cell item so the contents travel with it.
 *
 * @param contents every stored resource once; copied
 */
public record CellContents(List<ResourceAmount> contents) {

    public static final CellContents EMPTY = new CellContents(List.of());

    public static final Codec<CellContents> CODEC =
            NexusResources.AMOUNT_CODEC.listOf().xmap(CellContents::new, CellContents::contents);

    public static final StreamCodec<RegistryFriendlyByteBuf, CellContents> STREAM_CODEC =
            NexusResources.AMOUNT_STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(CellContents::new, CellContents::contents);

    public CellContents {
        contents = List.copyOf(Objects.requireNonNull(contents, "contents must not be null"));
    }
}
