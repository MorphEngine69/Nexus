package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.resource.FilterMatchMode;
import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.api.transport.SchedulingMode;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.transport.GroupEntry;
import com.morphengine.nexus.transport.PushEntry;
import com.morphengine.nexus.transport.StockEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What an attached device is set to in its panel, kept by the device. It keeps
 * a filter with its amounts for every kind of resource and uses the one of the
 * kind it moves, so switching kinds loses nothing. Settings a kind of device
 * does not offer keep their defaults and do nothing.
 *
 * @param lists     the filter and amounts of each kind of resource; copied, and
 *                  every kind gets one, fitted to it
 * @param resource  the one kind of resource the device moves
 * @param worldMode what a Placer or Remover does with items
 */
public record TransferSettings(
        Map<TransferResource, TransferList> lists, RedstoneMode redstone, SchedulingMode scheduling,
        DeliveryMode delivery, TransferResource resource, FilterMatchMode matchMode, WorldMode worldMode) {

    public static final TransferSettings DEFAULT = new TransferSettings(Map.of(), RedstoneMode.IGNORED,
            SchedulingMode.IN_ORDER, DeliveryMode.UNLIMITED, TransferResource.ITEM, FilterMatchMode.EXACT,
            WorldMode.BLOCKS);

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
                    NeoForgeStreamCodecs.enumCodec(WorldMode.class), TransferSettings::worldMode,
                    TransferSettings::new);

    public TransferSettings {
        Objects.requireNonNull(lists, "lists must not be null");
        Objects.requireNonNull(redstone, "redstone must not be null");
        Objects.requireNonNull(scheduling, "scheduling must not be null");
        Objects.requireNonNull(delivery, "delivery must not be null");
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(matchMode, "matchMode must not be null");
        Objects.requireNonNull(worldMode, "worldMode must not be null");
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
        return new TransferSettings(lists, redstone, scheduling, delivery, newResource, matchMode, worldMode);
    }

    public TransferSettings withRedstone(final RedstoneMode newRedstone) {
        return new TransferSettings(lists, newRedstone, scheduling, delivery, resource, matchMode, worldMode);
    }

    public TransferSettings withScheduling(final SchedulingMode newScheduling) {
        return new TransferSettings(lists, redstone, newScheduling, delivery, resource, matchMode, worldMode);
    }

    public TransferSettings withDelivery(final DeliveryMode newDelivery) {
        return new TransferSettings(lists, redstone, scheduling, newDelivery, resource, matchMode, worldMode);
    }

    /**
     * Changes how closely the filter compares a resource against what it
     * lists; unlike the filter itself, this is one setting for every kind.
     */
    public TransferSettings withMatchMode(final FilterMatchMode newMatchMode) {
        return new TransferSettings(lists, redstone, scheduling, delivery, resource, newMatchMode, worldMode);
    }

    public TransferSettings withWorldMode(final WorldMode newWorldMode) {
        return new TransferSettings(lists, redstone, scheduling, delivery, resource, matchMode, newWorldMode);
    }

    /**
     * @return the resources the filter lists by themselves, in slot order, each
     *         with the amount kept in stock when the settings keep stock, or
     *         without limit otherwise; a slot listing a tag names no one
     *         resource to count, so it is left out
     */
    public List<StockEntry> stock() {
        final List<FilterSlots.Entry> listed = filter().inSlotOrder();
        final List<StockEntry> entries = new ArrayList<>(listed.size());
        for (FilterSlots.Entry entry : listed) {
            if (entry.tag() == null) {
                entries.add(stockOf(entry));
            }
        }
        return entries;
    }

    /**
     * @return what a whitelist delivers, in slot order: the resources as in
     *         {@link #stock()} and, unless the settings keep stock, every tag
     *         the filter lists
     */
    public List<PushEntry> deliveries() {
        final List<FilterSlots.Entry> listed = filter().inSlotOrder();
        final List<PushEntry> entries = new ArrayList<>(listed.size());
        for (FilterSlots.Entry entry : listed) {
            final Optional<ResourceGroup> group = entry.group();
            if (group.isEmpty()) {
                entries.add(stockOf(entry));
            } else if (delivery != DeliveryMode.KEEP_STOCKED) {
                entries.add(new GroupEntry(group.get()));
            }
        }
        return entries;
    }

    private StockEntry stockOf(final FilterSlots.Entry entry) {
        return delivery == DeliveryMode.KEEP_STOCKED
                ? new StockEntry(entry.resource(), keepAmount(entry.slot(), entry.resource()))
                : StockEntry.unlimited(entry.resource());
    }

    private TransferSettings withList(final TransferList changed) {
        final Map<TransferResource, TransferList> updated = new EnumMap<>(lists);
        updated.put(resource, changed);
        return new TransferSettings(updated, redstone, scheduling, delivery, resource, matchMode, worldMode);
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
     * @param single   the one filter of such an old device; empty otherwise
     * @param resource {@code null} when not saved
     */
    private record Saved(
            Map<TransferResource, TransferList> lists, TransferList single,
            RedstoneMode redstone, SchedulingMode scheduling, DeliveryMode delivery,
            @Nullable TransferResource resource, FilterMatchMode matchMode, WorldMode worldMode) {

        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.unboundedMap(TransferResource.CODEC, TransferList.CODEC)
                                .optionalFieldOf("lists", Map.of()).forGetter(Saved::lists),
                        TransferList.MAP_CODEC.forGetter(Saved::single),
                        enumCodec(RedstoneMode.class).optionalFieldOf("redstone", DEFAULT.redstone())
                                .forGetter(Saved::redstone),
                        enumCodec(SchedulingMode.class).optionalFieldOf("scheduling", DEFAULT.scheduling())
                                .forGetter(Saved::scheduling),
                        DeliveryMode.CODEC.optionalFieldOf("delivery", DEFAULT.delivery()).forGetter(Saved::delivery),
                        TransferResource.CODEC.optionalFieldOf("resource")
                                .forGetter(saved -> Optional.ofNullable(saved.resource())),
                        enumCodec(FilterMatchMode.class).optionalFieldOf("match_mode", DEFAULT.matchMode())
                                .forGetter(Saved::matchMode),
                        WorldMode.CODEC.optionalFieldOf("world_mode", DEFAULT.worldMode()).forGetter(Saved::worldMode))
                .apply(instance, (lists, single, redstone, scheduling, delivery, resource, matchMode, worldMode) ->
                        new Saved(lists, single, redstone, scheduling, delivery, resource.orElse(null), matchMode,
                                worldMode)));

        static Saved of(final TransferSettings settings) {
            return new Saved(settings.lists(), TransferList.EMPTY, settings.redstone(),
                    settings.scheduling(), settings.delivery(), settings.resource(), settings.matchMode(),
                    settings.worldMode());
        }

        TransferSettings toSettings() {
            if (!lists.isEmpty()) {
                return new TransferSettings(lists, redstone, scheduling, delivery,
                        resource != null ? resource : TransferResource.ITEM, matchMode, worldMode);
            }
            final Map<TransferResource, TransferList> split = new EnumMap<>(TransferResource.class);
            for (TransferResource kind : TransferResource.values()) {
                split.put(kind, single);
            }
            return new TransferSettings(split, redstone, scheduling, delivery,
                    resource != null ? resource : TransferResource.inferredFrom(single.filter()), matchMode, worldMode);
        }
    }
}
