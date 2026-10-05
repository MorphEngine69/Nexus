package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.machine.MachineRecipes;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.math.SaturatedMath;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One line of a machine: an input slot, an output slot and the work between them. It starts a recipe when the input
 * slot holds what the recipe needs and the output slot has room for the result, works on it for as long as the
 * recipe says at the speed of the machine, using FE in each tick, and when done takes the inputs and gives the
 * result. A tick without enough FE does no work. Server thread only.
 */
public final class MachineLine {

    private static final long PERCENT = 100;

    private final List<MachineSlot> inputs;
    private final MachineSlot output;
    private final MachineRecipes recipes;
    private @Nullable MachineRecipe running;
    private long progress;
    private long restoredProgress;

    public MachineLine(final MachineSlot input, final MachineSlot output, final MachineRecipes recipes) {
        this(List.of(input), output, recipes);
    }

    /**
     * @param inputs the input slots, at least one; the recipe may take from any of them
     */
    public MachineLine(final List<MachineSlot> inputs, final MachineSlot output, final MachineRecipes recipes) {
        this.inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs must not be null"));
        if (this.inputs.isEmpty()) {
            throw new IllegalArgumentException("a line needs at least one input slot");
        }
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.recipes = Objects.requireNonNull(recipes, "recipes must not be null");
    }

    /**
     * Works for one game tick.
     *
     * @param speedPercent speed in percent of the time of the recipe, positive; a faster line uses
     *                     proportionally more FE in each tick, so a recipe costs the same at any speed
     * @return what the line did
     */
    public MachineActivity tick(final EnergyBuffer energy, final int speedPercent) {
        if (speedPercent <= 0) {
            throw new IllegalArgumentException("speedPercent must be positive: " + speedPercent);
        }
        if (running == null) {
            final Optional<MachineRecipe> found = findRecipe();
            if (found.isEmpty()) {
                return MachineActivity.IDLE;
            }
            if (!hasRoomFor(found.get())) {
                return MachineActivity.OUTPUT_BLOCKED;
            }
            begin(found.get());
        } else if (!inputsCover(running)) {
            cancel();
            return MachineActivity.IDLE;
        }
        return work(energy, speedPercent);
    }

    public boolean isRunning() {
        return running != null;
    }

    /**
     * @return the recipe the line is working on; empty when it is not running
     */
    public Optional<MachineRecipe> activeRecipe() {
        return Optional.ofNullable(running);
    }

    /**
     * @return how far the work is, from 0 to 100; zero when the line is not running
     */
    public int progressPercent() {
        final MachineRecipe recipe = running;
        return recipe == null ? 0 : (int) (progress * PERCENT / totalOf(recipe));
    }

    /**
     * @return the work done in hundredths of a tick of the recipe, to save; {@link #restoreProgress} takes it back
     */
    public long progress() {
        return progress;
    }

    /**
     * Gives back saved work: the line takes it up when it starts the recipe again, and drops it if the inputs are no
     * longer there.
     */
    public void restoreProgress(final long saved) {
        restoredProgress = Math.max(0, saved);
    }

    private Optional<MachineRecipe> findRecipe() {
        final List<ResourceAmount> held = held();
        return held.isEmpty() ? Optional.empty() : recipes.find(held);
    }

    private boolean hasRoomFor(final MachineRecipe recipe) {
        final ResourceAmount result = recipe.output();
        return output.room(result.resource()) >= result.amount();
    }

    /**
     * @return what the input slots hold, each resource once with the amount of all the slots that hold it
     */
    private List<ResourceAmount> held() {
        final Map<ResourceKey, Long> totals = new LinkedHashMap<>();
        for (MachineSlot slot : inputs) {
            slot.contents().ifPresent(contents -> totals.merge(contents.resource(), contents.amount(), Long::sum));
        }
        final List<ResourceAmount> held = new ArrayList<>(totals.size());
        totals.forEach((resource, amount) -> held.add(new ResourceAmount(resource, amount)));
        return held;
    }

    private boolean inputsCover(final MachineRecipe recipe) {
        final List<ResourceAmount> held = held();
        for (ResourceAmount needed : recipe.inputs()) {
            final boolean covered = held.stream().anyMatch(
                    have -> have.resource().equals(needed.resource()) && have.amount() >= needed.amount());
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    private void begin(final MachineRecipe recipe) {
        running = recipe;
        progress = Math.min(restoredProgress, totalOf(recipe));
        restoredProgress = 0;
    }

    private void cancel() {
        running = null;
        progress = 0;
    }

    private MachineActivity work(final EnergyBuffer energy, final int speedPercent) {
        final MachineRecipe recipe = Objects.requireNonNull(running);
        final long total = totalOf(recipe);
        if (progress < total) {
            final long cost = Math.ceilDiv(SaturatedMath.multiply(recipe.energyPerTick(), speedPercent), PERCENT);
            if (energy.extract(cost, Action.SIMULATE) < cost) {
                return MachineActivity.WAITING_FOR_ENERGY;
            }
            energy.extract(cost, Action.EXECUTE);
            progress = Math.min(total, progress + speedPercent);
        }
        if (progress < total) {
            return MachineActivity.WORKING;
        }
        return finish(recipe) ? MachineActivity.WORKING : MachineActivity.OUTPUT_BLOCKED;
    }

    private boolean finish(final MachineRecipe recipe) {
        if (!hasRoomFor(recipe)) {
            return false;
        }
        for (ResourceAmount used : recipe.inputs()) {
            takeFromInputs(used);
        }
        output.insert(recipe.output().resource(), recipe.output().amount(), Action.EXECUTE);
        cancel();
        return true;
    }

    private void takeFromInputs(final ResourceAmount used) {
        long left = used.amount();
        for (MachineSlot slot : inputs) {
            if (left > 0) {
                left -= slot.extract(used.resource(), left, Action.EXECUTE);
            }
        }
    }

    private static long totalOf(final MachineRecipe recipe) {
        return recipe.ticks() * PERCENT;
    }
}
