package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/**
 * What a Puller or Pusher does with one kind of resource: which resources its
 * filter lists and how much of each it keeps in stock.
 */
public record TransferList(FilterSlots filter, KeepAmounts keep) {

    public static final TransferList EMPTY = new TransferList(FilterSlots.EMPTY, KeepAmounts.NONE);

    public static final Codec<TransferList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FilterSlots.CODEC.optionalFieldOf("filter", FilterSlots.EMPTY).forGetter(TransferList::filter),
                    KeepAmounts.CODEC.optionalFieldOf("keep", KeepAmounts.NONE).forGetter(TransferList::keep))
            .apply(instance, TransferList::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferList> STREAM_CODEC = StreamCodec.composite(
            FilterSlots.STREAM_CODEC, TransferList::filter,
            KeepAmounts.STREAM_CODEC, TransferList::keep,
            TransferList::new);

    public TransferList {
        Objects.requireNonNull(filter, "filter must not be null");
        Objects.requireNonNull(keep, "keep must not be null");
    }

    /**
     * A slot that now lists another resource keeps one step of it again.
     */
    public TransferList withFilter(final FilterSlots newFilter) {
        KeepAmounts kept = keep;
        for (int slot = 0; slot < FilterSlots.MAX_SLOTS; slot++) {
            if (!Objects.equals(filter.resourceAt(slot), newFilter.resourceAt(slot))) {
                kept = kept.without(slot);
            }
        }
        return new TransferList(newFilter, kept);
    }

    /**
     * @return units of {@code listed}, the resource in {@code slot}, to keep in stock
     */
    public long keepAmount(final int slot, final NexusResource listed) {
        return keep.of(slot, listed);
    }

    public TransferList withKeepAmount(final int slot, final long amount) {
        return new TransferList(filter, keep.with(slot, amount));
    }

    /**
     * @return this list with only what fits {@code resource}: the entries of its
     *         kind, and the amounts of the slots that still list the same resource
     */
    TransferList fittedTo(final TransferResource resource) {
        return withFilter(resource.normalize(filter));
    }
}
