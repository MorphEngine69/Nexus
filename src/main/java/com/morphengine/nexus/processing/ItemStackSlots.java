package com.morphengine.nexus.processing;

import com.morphengine.nexus.machine.MachineSlot;
import com.morphengine.nexus.machine.MachineSlots;
import com.morphengine.nexus.machine.MachineTier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The slots of a machine as two containers of stacks, one for the inputs and one for the outputs, big enough for the
 * highest tier: the menu shows the slots of the tier the machine has, the machine core works on them through
 * {@link MachineSlot}s that stand for the stacks, and the stacks are what is saved. Server thread only.
 */
public final class ItemStackSlots implements MachineSlots {

    /** Lines of the highest tier, which the containers have room for. */
    public static final int MAX_LINES = MachineTier.QUANTUM.linePairs();

    private final Container inputs;
    private final Container outputs;
    private final List<MachineSlot> inputSlots = new ArrayList<>();
    private final List<MachineSlot> outputSlots = new ArrayList<>();

    /**
     * @param changed called when anything in either container changes
     */
    public ItemStackSlots(final Runnable changed) {
        Objects.requireNonNull(changed, "changed must not be null");
        this.inputs = new Notifying(changed);
        this.outputs = new Notifying(changed);
        for (int index = 0; index < MAX_LINES; index++) {
            inputSlots.add(new ItemStackMachineSlot(inputs, index));
            outputSlots.add(new ItemStackMachineSlot(outputs, index));
        }
    }

    @Override
    public MachineSlot input(final int index) {
        return inputSlots.get(index);
    }

    @Override
    public MachineSlot output(final int index) {
        return outputSlots.get(index);
    }

    public Container inputs() {
        return inputs;
    }

    public Container outputs() {
        return outputs;
    }

    public NonNullList<ItemStack> inputStacks() {
        return ((SimpleContainer) inputs).getItems();
    }

    public NonNullList<ItemStack> outputStacks() {
        return ((SimpleContainer) outputs).getItems();
    }

    /** A container that reports every change. */
    private static final class Notifying extends SimpleContainer {

        private final Runnable changed;

        Notifying(final Runnable changed) {
            super(MAX_LINES);
            this.changed = changed;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            changed.run();
        }
    }
}
