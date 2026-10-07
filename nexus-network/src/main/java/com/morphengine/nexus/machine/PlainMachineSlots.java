package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * Slots that keep their contents themselves, made when first asked for. Server thread only.
 */
public final class PlainMachineSlots implements MachineSlots {

    private final ToLongFunction<ResourceKey> limit;
    private final List<MachineSlot> inputs = new ArrayList<>();
    private final List<MachineSlot> outputs = new ArrayList<>();

    /**
     * @param limit how many units of a resource a slot holds at most
     */
    public PlainMachineSlots(final ToLongFunction<ResourceKey> limit) {
        this.limit = Objects.requireNonNull(limit, "limit must not be null");
    }

    @Override
    public MachineSlot input(final int index) {
        return slot(inputs, index);
    }

    @Override
    public MachineSlot output(final int index) {
        return slot(outputs, index);
    }

    private MachineSlot slot(final List<MachineSlot> slots, final int index) {
        while (slots.size() <= index) {
            slots.add(new PlainMachineSlot(limit));
        }
        return slots.get(index);
    }
}
