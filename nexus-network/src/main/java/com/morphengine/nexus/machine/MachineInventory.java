package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.ExtractableStorage;
import com.morphengine.nexus.api.storage.InsertableStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The slots of a machine as the two storages a Puller, a Pusher or an Assembler sees: what is put in is spread over the
 * input slots as the {@link InputMode} says, what is taken out comes from the output slots. Server thread only.
 */
public final class MachineInventory implements InsertableStorage, ExtractableStorage {

    private final MachineSlots slots;
    private final int inputsPerLine;
    private final List<MachineSlot> inputs = new ArrayList<>();
    private final List<MachineSlot> outputs = new ArrayList<>();
    private InputMode mode = InputMode.PER_RESOURCE;

    /**
     * @param lines how many lines, each with one input slot and one output slot, the machine starts with, positive
     * @param slots where the slots come from
     */
    public MachineInventory(final int lines, final MachineSlots slots) {
        this(lines, 1, slots);
    }

    /**
     * @param lines         how many lines the machine starts with, positive
     * @param inputsPerLine input slots of each line, positive; a line has one output slot
     * @param slots         where the slots come from; the input slots of line {@code n} are numbered
     *                      {@code n * inputsPerLine} up
     */
    public MachineInventory(final int lines, final int inputsPerLine, final MachineSlots slots) {
        if (inputsPerLine <= 0) {
            throw new IllegalArgumentException("inputsPerLine must be positive: " + inputsPerLine);
        }
        this.inputsPerLine = inputsPerLine;
        this.slots = Objects.requireNonNull(slots, "slots must not be null");
        grow(lines);
    }

    /**
     * Adds slots until there are {@code lines} lines; the slots already there keep what they hold.
     *
     * @throws IllegalArgumentException if {@code lines} is less than there are now
     */
    public void grow(final int lines) {
        if (lines < outputs.size() || lines <= 0) {
            throw new IllegalArgumentException("a machine cannot go from " + outputs.size() + " to " + lines
                    + " lines");
        }
        while (outputs.size() < lines) {
            for (int slot = 0; slot < inputsPerLine; slot++) {
                inputs.add(slots.input(inputs.size()));
            }
            outputs.add(slots.output(outputs.size()));
        }
    }

    public int lineCount() {
        return outputs.size();
    }

    public int inputsPerLine() {
        return inputsPerLine;
    }

    /**
     * @return the input slots of line {@code line}, in order
     */
    public List<MachineSlot> inputsOf(final int line) {
        return List.copyOf(inputs.subList(line * inputsPerLine, (line + 1) * inputsPerLine));
    }

    public int inputCount() {
        return inputs.size();
    }

    public MachineSlot input(final int index) {
        return inputs.get(index);
    }

    public MachineSlot output(final int index) {
        return outputs.get(index);
    }

    public InputMode mode() {
        return mode;
    }

    public void setMode(final InputMode newMode) {
        mode = Objects.requireNonNull(newMode, "mode must not be null");
    }

    /**
     * Puts the resource into the input slots as the {@linkplain #mode() mode} says; the actor is not looked at.
     */
    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long[] parts = planInsert(resource, amount);
        long accepted = 0;
        for (int index = 0; index < parts.length; index++) {
            if (parts[index] > 0) {
                accepted += inputs.get(index).insert(resource, parts[index], action);
            }
        }
        return accepted;
    }

    /**
     * Works out how an insert of {@code amount} of {@code resource} is spread over the input slots, as the
     * {@linkplain #mode() mode} says, without changing anything.
     *
     * @param amount units offered, must be positive
     * @return for every input slot the units it would take, zero for those that take none; the total may be less than
     *         {@code amount}, down to nothing
     */
    public long[] planInsert(final ResourceKey resource, final long amount) {
        requirePositive(amount);
        if (inputsPerLine > 1) {
            return planPerResource(resource, amount);
        }
        return switch (mode) {
            case PER_RESOURCE -> planPerResource(resource, amount);
            case SPLIT -> planSplit(resource, amount);
        };
    }

    /**
     * Takes the resource out of the output slots that hold it; the actor is not looked at.
     */
    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        requirePositive(amount);
        long taken = 0;
        for (MachineSlot slot : outputs) {
            if (taken < amount) {
                taken += slot.extract(resource, amount - taken, action);
            }
        }
        return taken;
    }

    private long[] planPerResource(final ResourceKey resource, final long amount) {
        final long[] parts = new long[inputs.size()];
        int target = -1;
        for (int index = 0; index < parts.length && target < 0; index++) {
            if (resource.equals(inputs.get(index).resource())) {
                target = index;
            }
        }
        for (int index = 0; index < parts.length && target < 0; index++) {
            if (inputs.get(index).isEmpty()) {
                target = index;
            }
        }
        if (target >= 0) {
            parts[target] = Math.min(amount, inputs.get(target).room(resource));
        }
        return parts;
    }

    private long[] planSplit(final ResourceKey resource, final long amount) {
        for (MachineSlot slot : inputs) {
            if (!slot.isEmpty() && !resource.equals(slot.resource())) {
                return new long[inputs.size()];
            }
        }
        return split(resource, amount);
    }

    /**
     * Shares {@code amount} over the input slots that have room so that they end up as even as they can: the slots
     * that hold the least are filled up first, the odd units go to the first of the slots at the same level, and what
     * a full slot cannot take goes to the others. So a stack that arrives one unit at a time spreads just as a whole
     * stack does.
     */
    private long[] split(final ResourceKey resource, final long amount) {
        final long[] parts = new long[inputs.size()];
        long remaining = amount;
        while (remaining > 0) {
            final List<Integer> lowest = lowestWithRoom(resource, parts);
            if (lowest.isEmpty()) {
                break;
            }
            remaining -= fillLowest(resource, parts, lowest, remaining);
        }
        return parts;
    }

    /**
     * @return the slots with room that hold the least once {@code parts} are in, in slot order
     */
    private List<Integer> lowestWithRoom(final ResourceKey resource, final long[] parts) {
        final List<Integer> lowest = new ArrayList<>();
        long lowestLevel = Long.MAX_VALUE;
        for (int index = 0; index < parts.length; index++) {
            if (inputs.get(index).room(resource) <= parts[index]) {
                continue;
            }
            final long level = inputs.get(index).amount() + parts[index];
            if (level < lowestLevel) {
                lowestLevel = level;
                lowest.clear();
            }
            if (level == lowestLevel) {
                lowest.add(index);
            }
        }
        return lowest;
    }

    /**
     * Gives the lowest slots units up to the level of the next slot above them, or all that is left when it is less.
     *
     * @return the units given
     */
    private long fillLowest(
            final ResourceKey resource, final long[] parts, final List<Integer> lowest, final long remaining) {
        final long step = Math.min(levelAbove(resource, parts, lowest), roomLeft(resource, parts, lowest));
        final long wanted = step * lowest.size();
        final long given = Math.min(wanted, remaining);
        final long share = given / lowest.size();
        long extra = given % lowest.size();
        for (int index : lowest) {
            parts[index] += share + (extra > 0 ? 1 : 0);
            extra = Math.max(0, extra - 1);
        }
        return given;
    }

    private long levelAbove(final ResourceKey resource, final long[] parts, final List<Integer> lowest) {
        final int first = lowest.getFirst();
        final long lowestLevel = inputs.get(first).amount() + parts[first];
        long above = Long.MAX_VALUE;
        for (int index = 0; index < parts.length; index++) {
            final long level = inputs.get(index).amount() + parts[index];
            if (level > lowestLevel && inputs.get(index).room(resource) > parts[index]) {
                above = Math.min(above, level);
            }
        }
        return above == Long.MAX_VALUE ? Long.MAX_VALUE : above - lowestLevel;
    }

    private long roomLeft(final ResourceKey resource, final long[] parts, final List<Integer> lowest) {
        long left = Long.MAX_VALUE;
        for (int index : lowest) {
            left = Math.min(left, inputs.get(index).room(resource) - parts[index]);
        }
        return left;
    }

    private static void requirePositive(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }
}
