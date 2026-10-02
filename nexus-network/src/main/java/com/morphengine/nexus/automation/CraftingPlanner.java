package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.StorageView;
import com.morphengine.nexus.math.SaturatedMath;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Works out what crafting an amount of a resource takes. The resource asked
 * for is always crafted, never taken from storage; every ingredient is taken
 * from what earlier runs of the plan gave beyond their need, then from
 * storage, and only what is still lacking is crafted in turn. Inputs are
 * served in their {@link InputOrder}; one with substitutes takes whatever of
 * the resources it accepts is at hand, in the order they are listed, and
 * crafts what is lacking as the first one some blueprint gives. Of several
 * blueprints for one resource, the first that leaves nothing missing is used;
 * when none does, the first one, so the plan shows what it lacks.
 *
 * <p>A blueprint that needs, however deep down, the resource it is crafting
 * could never start, so such a loop counts as missing. So do trees deeper than
 * {@value #MAX_DEPTH} levels, plans that look at more than {@value
 * #MAX_REQUESTS} ingredients, and amounts too large for a {@code long}.
 * Reads the storage only; server thread only.
 */
public final class CraftingPlanner {

    static final int MAX_DEPTH = 64;
    static final int MAX_REQUESTS = 100_000;

    private final BlueprintProvider blueprints;
    private final StorageView storage;
    private final Deque<ResourceKey> path = new ArrayDeque<>();
    private int requests;

    /**
     * @param storage the network's storage, read as it stands while planning
     */
    public CraftingPlanner(final BlueprintProvider blueprints, final StorageView storage) {
        this.blueprints = Objects.requireNonNull(blueprints, "blueprints must not be null");
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
    }

    /**
     * @param amount units asked for, must be positive
     */
    public CraftingPlan plan(final ResourceKey resource, final long amount) {
        final ResourceAmount target = new ResourceAmount(resource, amount);
        final PlanSimulation simulation = new PlanSimulation(storage);
        path.clear();
        requests = 0;
        craft(simulation, resource, amount);
        return simulation.toPlan(target);
    }

    /**
     * Finds the most of {@code resource}, up to {@code amount}, that can be
     * crafted with nothing missing. More units never take less, so the amount
     * is found by halving the range between one that works and one that does
     * not; each step plans in full, so this costs about {@code log2(amount)}
     * plans.
     *
     * @param amount units asked for, must be positive
     * @return the plan for the largest amount that can start; the plan for
     *         {@code amount} itself when not even one unit can
     */
    public CraftingPlan planLargest(final ResourceKey resource, final long amount) {
        final CraftingPlan asked = plan(resource, amount);
        if (asked.isComplete()) {
            return asked;
        }
        @Nullable CraftingPlan best = null;
        long works = 0;
        long fails = amount;
        while (fails - works > 1) {
            final long middle = works + (fails - works) / 2;
            final CraftingPlan attempt = plan(resource, middle);
            if (attempt.isComplete()) {
                works = middle;
                best = attempt;
            } else {
                fails = middle;
            }
        }
        return best != null ? best : asked;
    }

    private void request(final PlanSimulation simulation, final BlueprintInput input, final long amount) {
        long lacking = amount;
        for (int i = 0; i < input.options().size() && lacking > 0; i++) {
            lacking -= simulation.take(input.options().get(i), lacking);
        }
        if (lacking > 0) {
            craft(simulation, craftableOption(input), lacking);
        }
    }

    /**
     * @return the first resource the input accepts that some blueprint gives;
     *         the one it prefers when none is, so the plan shows that one missing
     */
    private ResourceKey craftableOption(final BlueprintInput input) {
        for (ResourceKey option : input.options()) {
            if (!blueprints.blueprintsFor(option).isEmpty() && !path.contains(option)) {
                return option;
            }
        }
        return input.preferred();
    }

    private void craft(final PlanSimulation simulation, final ResourceKey resource, final long amount) {
        requests++;
        final List<Blueprint> candidates = blueprints.blueprintsFor(resource);
        if (candidates.isEmpty() || isOutOfReach(resource)) {
            simulation.miss(resource, amount);
            return;
        }
        if (candidates.size() > 1) {
            final long missingBefore = simulation.missingTotal();
            for (Blueprint candidate : candidates) {
                final PlanSimulation attempt = simulation.copy();
                craftWith(attempt, candidate, resource, amount);
                if (attempt.missingTotal() == missingBefore) {
                    simulation.takeOver(attempt);
                    return;
                }
            }
        }
        craftWith(simulation, candidates.getFirst(), resource, amount);
    }

    /**
     * @return whether crafting {@code resource} here would loop back on itself or
     *         go past the limits of a plan
     */
    private boolean isOutOfReach(final ResourceKey resource) {
        return requests > MAX_REQUESTS || path.size() >= MAX_DEPTH || path.contains(resource);
    }

    private void craftWith(
            final PlanSimulation simulation, final Blueprint blueprint, final ResourceKey resource,
            final long amount) {
        final long perRun = blueprint.outputOf(resource);
        final long runs = (amount - 1) / perRun + 1;
        if (overflows(blueprint, runs)) {
            simulation.miss(resource, amount);
            return;
        }
        path.push(resource);
        for (BlueprintInput input : InputOrder.narrowestFirst(blueprint)) {
            request(simulation, input, input.amount() * runs);
        }
        path.pop();
        simulation.run(blueprint, runs);
        simulation.take(resource, amount);
    }

    /**
     * @return whether {@code runs} runs would take or give more units in all than a {@code long} holds
     */
    private static boolean overflows(final Blueprint blueprint, final long runs) {
        long perRun = 0;
        for (BlueprintInput input : blueprint.inputs()) {
            perRun = SaturatedMath.add(perRun, input.amount());
        }
        for (ResourceAmount output : blueprint.outputs()) {
            perRun = SaturatedMath.add(perRun, output.amount());
        }
        return SaturatedMath.multiply(perRun, runs) == Long.MAX_VALUE;
    }
}
