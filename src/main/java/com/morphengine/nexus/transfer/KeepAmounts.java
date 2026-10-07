package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How much of the resource in each filter slot a Pusher keeps the storage
 * beside it stocked with. A slot without an amount of its own keeps one
 * {@linkplain com.morphengine.nexus.resource.AmountUnit#step step} of its
 * resource: one item, one bucket, 10,000 FE.
 *
 * @param bySlot units set for a slot, each in [1, {@value #MAX}]; copied
 */
public record KeepAmounts(Map<Integer, Long> bySlot) {

    /** The most units a slot keeps. */
    public static final long MAX = Integer.MAX_VALUE;
    public static final KeepAmounts NONE = new KeepAmounts(Map.of());

    public static final Codec<KeepAmounts> CODEC = Slot.CODEC.listOf().xmap(
            slots -> new KeepAmounts(toMap(slots)), amounts -> toList(amounts.bySlot()));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeepAmounts> STREAM_CODEC =
            ByteBufCodecs.map(HashMap<Integer, Long>::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_LONG,
                            FilterSlots.MAX_SLOTS)
                    .<KeepAmounts>map(KeepAmounts::new, amounts -> new HashMap<>(amounts.bySlot())).cast();

    public KeepAmounts {
        bySlot = Map.copyOf(bySlot);
        for (Map.Entry<Integer, Long> amount : bySlot.entrySet()) {
            if (amount.getValue() < 1 || amount.getValue() > MAX) {
                throw new IllegalArgumentException("amount to keep out of range [1, " + MAX + "] in slot "
                        + amount.getKey() + ": " + amount.getValue());
            }
        }
    }

    /**
     * @return units of {@code resource}, listed in {@code slot}, to keep stocked
     */
    public long of(final int slot, final NexusResource resource) {
        final Long set = bySlot.get(slot);
        return set != null ? set : resource.type().unit().step();
    }

    /**
     * @param amount units to keep; clamped to [1, {@value #MAX}]
     */
    public KeepAmounts with(final int slot, final long amount) {
        final Map<Integer, Long> updated = new HashMap<>(bySlot);
        updated.put(slot, Math.clamp(amount, 1, MAX));
        return new KeepAmounts(updated);
    }

    /**
     * @return these amounts with {@code slot} back to its default
     */
    public KeepAmounts without(final int slot) {
        if (!bySlot.containsKey(slot)) {
            return this;
        }
        final Map<Integer, Long> updated = new HashMap<>(bySlot);
        updated.remove(slot);
        return new KeepAmounts(updated);
    }

    private static Map<Integer, Long> toMap(final List<Slot> slots) {
        final Map<Integer, Long> map = new HashMap<>();
        for (Slot slot : slots) {
            map.put(slot.slot(), Math.clamp(slot.amount(), 1, MAX));
        }
        return map;
    }

    private static List<Slot> toList(final Map<Integer, Long> map) {
        final List<Slot> slots = new ArrayList<>(map.size());
        for (Map.Entry<Integer, Long> amount : map.entrySet()) {
            slots.add(new Slot(amount.getKey(), amount.getValue()));
        }
        return slots;
    }

    private record Slot(int slot, long amount) {

        static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.intRange(0, FilterSlots.MAX_SLOTS - 1).fieldOf("slot").forGetter(Slot::slot),
                        Codec.LONG.fieldOf("amount").forGetter(Slot::amount))
                .apply(instance, Slot::new));
    }
}
