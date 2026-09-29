package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.resource.FilterMatchMode;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.api.transport.SchedulingMode;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What a Puller or Pusher is set to in its panel, kept by the device. It keeps
 * a filter with its amounts for every kind of resource and uses the one of the
 * kind it moves, so switching kinds loses nothing. Settings a kind of device
 * does not offer keep their defaults and do nothing.
 *
 * @param lists    the filter and amounts of each kind of resource; copied, and
 *                 every kind gets one, fitted to it
 * @param resource the one kind of resource the device moves
 */
public record TransferSettings(
        Map<TransferResource, TransferList> lists, RedstoneMode redstone, SchedulingMode scheduling,
        DeliveryMode delivery, TransferResource resource, FilterMatchMode matchMode) {

    public static final TransferSettings DEFAULT = new TransferSettings(Map.of(), RedstoneMode.IGNORED,
            SchedulingMode.IN_ORDER, DeliveryMode.UNLIMITED, TransferResource.ITEM, FilterMatchMode.EXACT);

    public static final Codec<TransferSettings> CODEC = Saved.CODEC.xmap(Saved::toSettings, Saved::of);

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferSettings> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap<TransferResource, TransferList>::new,
                                    NeoForgeStreamCodecs.enumCodec(TransferResource.class), TransferList.STREAM_CODEC)
                            .<Map<TransferResource, TransferList>>map(lists -> lists, HashMap::new),
                    TransferSettings::lists,
                    NeoForgeStreamCodecs.enumCodec(RedstoneMode.class), TransferSettings::redstone,
                    NeoForgeStreamCodecs.enumCodec(SchedulingMode.class), TransferSettings::scheduling,
                    NeoForgeStreamCodecs.enumCodec(DeliveryMode.class), TransferSettings::delivery,
                    NeoForgeStreamCodecs.enumCodec(TransferResource.class), TransferSettings::resource,
                    NeoForgeStreamCodecs.enumCodec(FilterMatchMode.class), TransferSettings::matchMode,
                    TransferSettings::new);

    public TransferSettings {
        Objects.requireNonNull(lists, "lists must not be null");
        Objects.requireNonNull(redstone, "redstone must not be null");
        Objects.requireNonNull(scheduling, "scheduling must not be null");
        Objects.requireNonNull(delivery, "delivery must not be null");
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(matchMode, "matchMode must not be null");
        final Map<TransferResource, TransferList> fitted = new EnumMap<>(TransferResource.class);
        for (TransferResource kind : TransferResource.values()) {
            fitted.put(kind, lists.getOrDefault(kind, TransferList.EMPTY).fittedTo(kind));
        }
        lists = Map.copyOf(fitted);
    }

    /**
     * @return the filter and amounts of the kind of resource the device moves
     */
    public TransferList list() {
        return lists.get(resource);
    }

    public FilterSlots filter() {
        return list().filter();
    }

    public TransferSettings withFilter(final FilterSlots newFilter) {
        return withList(list().withFilter(newFilter));
    }

    /**
     * @return units of {@code listed}, the resource in {@code slot}, to keep in stock
     */
    public long keepAmount(final int slot, final NexusResource listed) {
        return list().keepAmount(slot, listed);
    }

    public TransferSettings withKeepAmount(final int slot, final long amount) {
        return withList(list().withKeepAmount(slot, amount));
    }

    /**
     * Switches to another kind of resource; the filter of every kind stays as it is.
     */
    public TransferSettings withResource(final TransferResource newResource) {
        return new TransferSettings(lists, redstone, scheduling, delivery, newResource, matchMode);
    }

    public TransferSettings withRedstone(final RedstoneMode newRedstone) {
        return new TransferSettings(lists, newRedstone, scheduling, delivery, resource, matchMode);
    }

    public TransferSettings withScheduling(final SchedulingMode newScheduling) {
        return new TransferSettings(lists, redstone, newScheduling, delivery, resource, matchMode);
    }

    public TransferSettings withDelivery(final DeliveryMode newDelivery) {
        return new TransferSettings(lists, redstone, scheduling, newDelivery, resource, matchMode);
    }

    /**
     * Changes how closely the filter compares a resource against what it
     * lists; unlike the filter itself, this is one setting for every kind.
     */
    public TransferSettings withMatchMode(final FilterMatchMode newMatchMode) {
        return new TransferSettings(lists, redstone, scheduling, delivery, resource, newMatchMode);
    }

    private TransferSettings withList(final TransferList changed) {
        final Map<TransferResource, TransferList> updated = new EnumMap<>(lists);
        updated.put(resource, changed);
        return new TransferSettings(updated, redstone, scheduling, delivery, resource, matchMode);
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

    /**
     * The settings as saved. A device saved before it kept a filter per kind
     * has a single {@code filter} and {@code keep} instead of {@code lists};
     * every kind then takes the entries of its own from them, so nothing is
     * lost. A device saved before it had a kind also has no {@code resource}
     * and takes the kind its filter lists first.
     *
     * @param lists    empty for such an old device
     * @param resource {@code null} when not saved
     */
    private record Saved(
            Map<TransferResource, TransferList> lists, FilterSlots filter, KeepAmounts keep,
            RedstoneMode redstone, SchedulingMode scheduling, DeliveryMode delivery,
            @Nullable TransferResource resource, FilterMatchMode matchMode) {

        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.unboundedMap(TransferResource.CODEC, TransferList.CODEC)
                                .optionalFieldOf("lists", Map.of()).forGetter(Saved::lists),
                        FilterSlots.CODEC.optionalFieldOf("filter", FilterSlots.EMPTY).forGetter(Saved::filter),
                        KeepAmounts.CODEC.optionalFieldOf("keep", KeepAmounts.NONE).forGetter(Saved::keep),
                        enumCodec(RedstoneMode.class).optionalFieldOf("redstone", DEFAULT.redstone())
                                .forGetter(Saved::redstone),
                        enumCodec(SchedulingMode.class).optionalFieldOf("scheduling", DEFAULT.scheduling())
                                .forGetter(Saved::scheduling),
                        DeliveryMode.CODEC.optionalFieldOf("delivery", DEFAULT.delivery()).forGetter(Saved::delivery),
                        TransferResource.CODEC.optionalFieldOf("resource")
                                .forGetter(saved -> Optional.ofNullable(saved.resource())),
                        enumCodec(FilterMatchMode.class).optionalFieldOf("match_mode", DEFAULT.matchMode())
                                .forGetter(Saved::matchMode))
                .apply(instance, (lists, filter, keep, redstone, scheduling, delivery, resource, matchMode) ->
                        new Saved(lists, filter, keep, redstone, scheduling, delivery, resource.orElse(null),
                                matchMode)));

        static Saved of(final TransferSettings settings) {
            return new Saved(settings.lists(), FilterSlots.EMPTY, KeepAmounts.NONE, settings.redstone(),
                    settings.scheduling(), settings.delivery(), settings.resource(), settings.matchMode());
        }

        TransferSettings toSettings() {
            if (!lists.isEmpty()) {
                return new TransferSettings(lists, redstone, scheduling, delivery,
                        resource != null ? resource : TransferResource.ITEM, matchMode);
            }
            final TransferList single = new TransferList(filter, keep);
            final Map<TransferResource, TransferList> split = new EnumMap<>(TransferResource.class);
            for (TransferResource kind : TransferResource.values()) {
                split.put(kind, single);
            }
            return new TransferSettings(split, redstone, scheduling, delivery,
                    resource != null ? resource : TransferResource.inferredFrom(filter), matchMode);
        }
    }
}
