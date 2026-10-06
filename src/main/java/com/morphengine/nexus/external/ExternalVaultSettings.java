package com.morphengine.nexus.external;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.storage.ExternalAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the player sets on an External Vault: which resources of the block it is set against the network may use, and
 * what the network may do with them.
 *
 * @param filter the resources listed, as a whitelist or a blacklist; an empty one lets everything pass
 * @param access whether the network may put resources into the block as well as take them out
 */
public record ExternalVaultSettings(FilterSlots filter, ExternalAccess access) {

    public static final ExternalVaultSettings DEFAULT = new ExternalVaultSettings(
            FilterSlots.EMPTY, ExternalAccess.READ_WRITE);

    private static final ExternalAccess[] ACCESSES = ExternalAccess.values();
    private static final Codec<ExternalAccess> ACCESS_CODEC = Codec.INT.xmap(
            index -> ACCESSES[Math.clamp(index, 0, ACCESSES.length - 1)], ExternalAccess::ordinal);

    public static final Codec<ExternalVaultSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FilterSlots.CODEC.optionalFieldOf("filter", FilterSlots.EMPTY)
                            .forGetter(ExternalVaultSettings::filter),
                    ACCESS_CODEC.optionalFieldOf("access", ExternalAccess.READ_WRITE)
                            .forGetter(ExternalVaultSettings::access))
            .apply(instance, ExternalVaultSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExternalVaultSettings> STREAM_CODEC =
            StreamCodec.composite(
                    FilterSlots.STREAM_CODEC, ExternalVaultSettings::filter,
                    ByteBufCodecs.idMapper(index -> ACCESSES[Math.clamp(index, 0, ACCESSES.length - 1)],
                            ExternalAccess::ordinal), ExternalVaultSettings::access,
                    ExternalVaultSettings::new);

    public ExternalVaultSettings withFilter(final FilterSlots changed) {
        return new ExternalVaultSettings(changed, access);
    }

    public ExternalVaultSettings withAccess(final ExternalAccess changed) {
        return new ExternalVaultSettings(filter, changed);
    }
}
