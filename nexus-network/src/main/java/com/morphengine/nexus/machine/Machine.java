package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.machine.MachineRecipes;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A machine that works on resources with FE: lines that each take an input slot to an output slot by the recipes of
 * the machine kind, a buffer of FE that only takes energy in, a tier that sets the number of lines, the buffer and
 * the speed, and settings for which side lets things in and out. What it does and which recipes it runs is the
 * business of whoever builds it; the same class serves a furnace, a crusher and the rest. Server thread only.
 */
public final class Machine {

    private final MachineShape shape;
    private final MachineRecipes recipes;
    private final SimpleEnergyBuffer energy;
    private final MachineInventory inventory;
    private final List<MachineLine> lines = new ArrayList<>();
    private MachineTier tier;
    private int speedUpgrades;
    private SideConfig<MachineSide> sides = MachineSides.defaults();
    private MachineActivity activity = MachineActivity.IDLE;

    /**
     * @param slots where the slots of the lines come from
     */
    public Machine(final MachineTier tier, final MachineRecipes recipes, final MachineSlots slots) {
        this(tier, MachineShape.SINGLE_INPUT, recipes, slots);
    }

    /**
     * @param shape how the lines are built, which also holds their number to the most the kind of machine has
     * @param slots where the slots of the lines come from
     */
    public Machine(
            final MachineTier tier, final MachineShape shape, final MachineRecipes recipes,
            final MachineSlots slots) {
        this.tier = Objects.requireNonNull(tier, "tier must not be null");
        this.shape = Objects.requireNonNull(shape, "shape must not be null");
        this.recipes = Objects.requireNonNull(recipes, "recipes must not be null");
        this.energy = new SimpleEnergyBuffer(tier.bufferCapacity(), tier.maxInsert(), tier.bufferCapacity());
        this.inventory = new MachineInventory(shape.linesOf(tier), shape.inputsPerLine(), slots);
        addLines();
    }

    public MachineTier tier() {
        return tier;
    }

    /**
     * Moves the machine up to a higher tier on the spot: more lines, a bigger buffer, a higher speed, and everything
     * in the slots and the buffer stays.
     *
     * @throws IllegalArgumentException unless {@code higher} is above the current tier
     */
    public void upgradeTo(final MachineTier higher) {
        if (higher.rank() <= tier.rank()) {
            throw new IllegalArgumentException("tier " + higher.rank() + " is not above tier " + tier.rank());
        }
        inventory.grow(shape.linesOf(higher));
        energy.resize(higher.bufferCapacity(), higher.maxInsert(), higher.bufferCapacity());
        tier = higher;
        addLines();
    }

    /**
     * @return the buffer; machines take FE in and give none, so a caller outside puts into it and never takes out
     */
    public SimpleEnergyBuffer energy() {
        return energy;
    }

    /**
     * @return the slots, for the player's menu, for an Assembler and for anything that is not bound by the sides
     */
    public MachineInventory inventory() {
        return inventory;
    }

    public int lineCount() {
        return lines.size();
    }

    public MachineLine line(final int index) {
        return lines.get(index);
    }

    public int speedUpgrades() {
        return speedUpgrades;
    }

    /**
     * @param count Speed Upgrades in the machine, from zero to {@link MachineSpeed#MAX_SPEED_UPGRADES}
     */
    public void setSpeedUpgrades(final int count) {
        MachineSpeed.percent(tier, count);
        speedUpgrades = count;
    }

    public int speedPercent() {
        return MachineSpeed.percent(tier, speedUpgrades);
    }

    /**
     * Works for one game tick: every line does what it can with the FE in the buffer, one after another.
     *
     * @return what the machine is doing, which is the most important of what its lines do
     */
    public MachineActivity tick() {
        final int speed = speedPercent();
        MachineActivity result = MachineActivity.IDLE;
        for (MachineLine line : lines) {
            result = result.or(line.tick(energy, speed));
        }
        activity = result;
        return result;
    }

    /**
     * A tick in which the machine does not work, because it may not: its lines stay as they are and it counts as idle.
     */
    public void hold() {
        activity = MachineActivity.IDLE;
    }

    /**
     * @return what the machine did in its last tick
     */
    public MachineActivity activity() {
        return activity;
    }

    public SideConfig<MachineSide> sides() {
        return sides;
    }

    public void setSideMode(final MachineSide side, final SideMode mode) {
        sides = sides.with(side, mode);
    }

    /**
     * Puts back saved settings of the sides; a damaged mode closes its side.
     *
     * @param bits what {@link SideConfig#toBits()} gave for the sides
     */
    public void restoreSides(final int bits) {
        sides = SideConfig.fromBits(MachineSide.class, bits);
    }

    /**
     * Puts something in from {@code side}, as a Pusher or a pipe would; a side that does not let input in takes
     * nothing.
     */
    public long insertFrom(
            final MachineSide side, final ResourceKey resource, final long amount, final Action action,
            final Actor actor) {
        return sides.mode(side).allowsInput() ? inventory.insert(resource, amount, action, actor) : 0;
    }

    /**
     * Takes something out through {@code side}, as a Puller or a pipe would; a side that does not let output out
     * gives nothing.
     */
    public long extractFrom(
            final MachineSide side, final ResourceKey resource, final long amount, final Action action,
            final Actor actor) {
        return sides.mode(side).allowsOutput() ? inventory.extract(resource, amount, action, actor) : 0;
    }

    private void addLines() {
        while (lines.size() < inventory.lineCount()) {
            final int index = lines.size();
            lines.add(new MachineLine(inventory.inputsOf(index), inventory.output(index), recipes));
        }
    }
}
