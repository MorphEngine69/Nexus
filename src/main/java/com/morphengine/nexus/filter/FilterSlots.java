package com.morphengine.nexus.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.resource.FilterMatchMode;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A filter a player sets: resources in fixed slots, as a whitelist or a
 * blacklist. Every resource is listed once. A slot can list the resource
 * itself or one of its tags, which stands for every resource in that tag. How many of the
 * {@value #MAX_SLOTS} slots are in use is up to the owner, such as 9 on a Vault
 * Cell.
 *
 * @param entries the occupied slots; copied
 */
public record FilterSlots(FilterMode mode, List<Entry> entries) {

    /** Slots any filter has at most. */
    public static final int MAX_SLOTS = 64;
    public static final FilterSlots EMPTY = new FilterSlots(FilterMode.ALLOW, List.of());

    private static final Codec<FilterMode> MODE_CODEC =
            Codec.BOOL.xmap(FilterSlots::modeOf, mode -> mode == FilterMode.ALLOW);
    private static final StreamCodec<RegistryFriendlyByteBuf, FilterMode> MODE_STREAM_CODEC =
            ByteBufCodecs.BOOL.<FilterMode>map(FilterSlots::modeOf, mode -> mode == FilterMode.ALLOW).cast();

    public static final Codec<FilterSlots> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    MODE_CODEC.fieldOf("whitelist").forGetter(FilterSlots::mode),
                    Entry.CODEC.listOf().fieldOf("entries").forGetter(FilterSlots::entries))
            .apply(instance, FilterSlots::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterSlots> STREAM_CODEC = StreamCodec.composite(
            MODE_STREAM_CODEC, FilterSlots::mode,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SLOTS)), FilterSlots::entries,
            FilterSlots::new);

    public FilterSlots {
        Objects.requireNonNull(mode, "mode must not be null");
        entries = List.copyOf(entries);
        final Set<Integer> slots = new HashSet<>();
        final Set<ResourceKey> resources = new HashSet<>();
        for (Entry entry : entries) {
            if (!slots.add(entry.slot()) || !resources.add(entry.resource())) {
                throw new IllegalArgumentException("filter lists a slot or resource twice: " + entries);
            }
        }
    }

    public @Nullable NexusResource resourceAt(final int slot) {
        final Entry entry = entryAt(slot);
        return entry == null ? null : entry.resource();
    }

    /**
     * @return what {@code slot} lists; {@code null} for an empty slot
     */
    public @Nullable Entry entryAt(final int slot) {
        for (Entry entry : entries) {
            if (entry.slot() == slot) {
                return entry;
            }
        }
        return null;
    }

    public boolean lists(final ResourceKey resource) {
        for (Entry entry : entries) {
            if (entry.resource().equals(resource)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param resource the new resource of the slot, listed by itself, not by a
     *                 tag; {@code null} empties it
     * @return this filter with {@code slot} set; unchanged if {@code resource}
     *         is already listed in another slot
     */
    public FilterSlots with(final int slot, final @Nullable NexusResource resource) {
        Entry.checkSlot(slot);
        if (resource != null && lists(resource)) {
            return this;
        }
        final List<Entry> updated = new ArrayList<>(entries.size() + 1);
        for (Entry entry : entries) {
            if (entry.slot() != slot) {
                updated.add(entry);
            }
        }
        if (resource != null) {
            updated.add(new Entry(slot, resource));
        }
        return new FilterSlots(mode, updated);
    }

    /**
     * Moves the slot on to the next tag of its resource, and from the last tag
     * back to the resource itself.
     *
     * @param step one to go forwards, minus one to go backwards
     * @return this filter with the tag of {@code slot} changed; unchanged for an
     *         empty slot and a resource without tags
     */
    public FilterSlots withNextTag(final int slot, final int step) {
        final List<Entry> updated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (Entry entry : entries) {
            if (entry.slot() == slot) {
                final Identifier next = entry.resource().nextTag(entry.tag(), step);
                changed = !Objects.equals(next, entry.tag());
                updated.add(new Entry(slot, entry.resource(), next));
            } else {
                updated.add(entry);
            }
        }
        return changed ? new FilterSlots(mode, updated) : this;
    }

    public FilterSlots withMode(final FilterMode newMode) {
        return new FilterSlots(newMode, entries);
    }

    /**
     * @return this filter with only the entries below {@code slots}; the rest
     *         stay out of matching without being dropped from what is saved
     */
    public FilterSlots limitedTo(final int slots) {
        final List<Entry> kept = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (entry.slot() < slots) {
                kept.add(entry);
            }
        }
        return kept.size() == entries.size() ? this : new FilterSlots(mode, kept);
    }

    /**
     * @return the entries in slot order
     */
    public List<Entry> inSlotOrder() {
        final List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort((first, second) -> Integer.compare(first.slot(), second.slot()));
        return List.copyOf(sorted);
    }

    public ResourceFilter toResourceFilter() {
        return toResourceFilter(FilterMatchMode.EXACT);
    }

    public ResourceFilter toResourceFilter(final FilterMatchMode matchMode) {
        final Set<ResourceKey> listed = new HashSet<>();
        final List<ResourceGroup> groups = new ArrayList<>();
        for (Entry entry : entries) {
            final Optional<ResourceGroup> group = entry.group();
            if (group.isPresent()) {
                groups.add(group.get());
            } else {
                listed.add(entry.resource());
            }
        }
        return new ResourceFilter(mode, matchMode, listed, groups);
    }

    private static FilterMode modeOf(final boolean whitelist) {
        return whitelist ? FilterMode.ALLOW : FilterMode.DENY;
    }

    /**
     * @param slot index of the filter slot, from zero to {@value FilterSlots#MAX_SLOTS} exclusive
     * @param tag  the tag of {@code resource} the slot stands for, such as
     *             {@code c:ores/iron}; {@code null} when it lists the resource itself
     */
    public record Entry(int slot, NexusResource resource, @Nullable Identifier tag) {

        /** A tag is saved only when there is one, so filters saved before tags existed read as they were. */
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.intRange(0, MAX_SLOTS - 1).fieldOf("slot").forGetter(Entry::slot),
                        NexusResources.CODEC.fieldOf("resource").forGetter(Entry::resource),
                        Identifier.CODEC.optionalFieldOf("tag").forGetter(Entry::tagIfAny))
                .apply(instance, (slot, resource, tag) -> new Entry(slot, resource, tag.orElse(null))));

        static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot,
                NexusResources.STREAM_CODEC, Entry::resource,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), Entry::tagIfAny,
                (slot, resource, tag) -> new Entry(slot, resource, tag.orElse(null)));

        public Entry {
            checkSlot(slot);
            Objects.requireNonNull(resource, "resource must not be null");
        }

        /**
         * A slot listing {@code resource} by itself.
         */
        public Entry(final int slot, final NexusResource resource) {
            this(slot, resource, null);
        }

        /**
         * @return every resource in the tag of the slot; empty when the slot
         *         lists its resource by itself or the resource has no tags
         */
        public Optional<ResourceGroup> group() {
            return tag == null ? Optional.empty() : resource.tagGroup(tag);
        }

        private Optional<Identifier> tagIfAny() {
            return Optional.ofNullable(tag);
        }

        static void checkSlot(final int slot) {
            if (slot < 0 || slot >= MAX_SLOTS) {
                throw new IllegalArgumentException("filter slot out of range [0, " + MAX_SLOTS + "): " + slot);
            }
        }
    }
}
