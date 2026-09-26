package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.api.transport.SchedulingMode;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Locale;
import java.util.Objects;

/**
 * What a Puller or Pusher is set to in its panel, kept by the device. Settings
 * a kind of device does not offer keep their defaults and do nothing.
 */
public record TransferSettings(
        FilterSlots filter, KeepAmounts keep, RedstoneMode redstone, SchedulingMode scheduling,
        DeliveryMode delivery) {

    public static final TransferSettings DEFAULT = new TransferSettings(FilterSlots.EMPTY, KeepAmounts.NONE,
            RedstoneMode.IGNORED, SchedulingMode.IN_ORDER, DeliveryMode.UNLIMITED);

    private static final Codec<RedstoneMode> REDSTONE_CODEC = enumCodec(RedstoneMode.class);
    private static final Codec<SchedulingMode> SCHEDULING_CODEC = enumCodec(SchedulingMode.class);

    public static final Codec<TransferSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FilterSlots.CODEC.optionalFieldOf("filter", DEFAULT.filter()).forGetter(TransferSettings::filter),
                    KeepAmounts.CODEC.optionalFieldOf("keep", DEFAULT.keep()).forGetter(TransferSettings::keep),
                    REDSTONE_CODEC.optionalFieldOf("redstone", DEFAULT.redstone())
                            .forGetter(TransferSettings::redstone),
                    SCHEDULING_CODEC.optionalFieldOf("scheduling", DEFAULT.scheduling())
                            .forGetter(TransferSettings::scheduling),
                    DeliveryMode.CODEC.optionalFieldOf("delivery", DEFAULT.delivery())
                            .forGetter(TransferSettings::delivery))
            .apply(instance, TransferSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferSettings> STREAM_CODEC =
            StreamCodec.composite(
                    FilterSlots.STREAM_CODEC, TransferSettings::filter,
                    KeepAmounts.STREAM_CODEC, TransferSettings::keep,
                    NeoForgeStreamCodecs.enumCodec(RedstoneMode.class), TransferSettings::redstone,
                    NeoForgeStreamCodecs.enumCodec(SchedulingMode.class), TransferSettings::scheduling,
                    NeoForgeStreamCodecs.enumCodec(DeliveryMode.class), TransferSettings::delivery,
                    TransferSettings::new);

    public TransferSettings {
        Objects.requireNonNull(filter, "filter must not be null");
        Objects.requireNonNull(keep, "keep must not be null");
        Objects.requireNonNull(redstone, "redstone must not be null");
        Objects.requireNonNull(scheduling, "scheduling must not be null");
        Objects.requireNonNull(delivery, "delivery must not be null");
    }

    /**
     * A slot that now lists another resource keeps one whole of it again.
     */
    public TransferSettings withFilter(final FilterSlots newFilter) {
        KeepAmounts kept = keep;
        for (int slot = 0; slot < FilterSlots.MAX_SLOTS; slot++) {
            if (!Objects.equals(filter.resourceAt(slot), newFilter.resourceAt(slot))) {
                kept = kept.without(slot);
            }
        }
        return new TransferSettings(newFilter, kept, redstone, scheduling, delivery);
    }

    /**
     * @return units of the resource in {@code slot} to keep stocked
     */
    public long keepAmount(final int slot, final NexusResource resource) {
        return keep.of(slot, resource);
    }

    public TransferSettings withKeepAmount(final int slot, final long amount) {
        return new TransferSettings(filter, keep.with(slot, amount), redstone, scheduling, delivery);
    }

    public TransferSettings withRedstone(final RedstoneMode newRedstone) {
        return new TransferSettings(filter, keep, newRedstone, scheduling, delivery);
    }

    public TransferSettings withScheduling(final SchedulingMode newScheduling) {
        return new TransferSettings(filter, keep, redstone, newScheduling, delivery);
    }

    public TransferSettings withDelivery(final DeliveryMode newDelivery) {
        return new TransferSettings(filter, keep, redstone, scheduling, newDelivery);
    }

    /**
     * Saves an enum of the core API, which knows nothing of Minecraft, by its
     * name in lower case.
     */
    private static <E extends Enum<E>> Codec<E> enumCodec(final Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            for (E value : type.getEnumConstants()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return DataResult.success(value);
                }
            }
            return DataResult.error(() -> "unknown " + type.getSimpleName() + ": " + name);
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }
}
