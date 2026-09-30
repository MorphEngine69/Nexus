package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.storage.ResourceCounter;

import java.util.List;

/**
 * The runs of one blueprint within a crafting task: how many are still to be
 * handed out, and which outputs are still awaited. An output counts against a
 * run only once that run is handed out, so two tasks running the same
 * blueprint never take each other's outputs. A run serves its inputs in their
 * {@link InputOrder}, each with whatever the task holds of the resources it
 * accepts, in the order they are listed.
 */
final class BlueprintRun {

    private final Blueprint blueprint;
    private final List<BlueprintInput> pickOrder;
    private final long totalRuns;
    private final ResourceCounter awaited = new ResourceCounter();
    private long toDispatch;
    private int nextExecutor;
    private DispatchResult problem = DispatchResult.ACCEPTED;

    BlueprintRun(final Blueprint blueprint, final long totalRuns) {
        this.blueprint = blueprint;
        this.pickOrder = InputOrder.narrowestFirst(blueprint);
        this.totalRuns = totalRuns;
        this.toDispatch = totalRuns;
        for (ResourceAmount output : blueprint.outputs()) {
            awaited.add(output.resource(), Math.multiplyExact(output.amount(), totalRuns));
        }
    }

    BlueprintRun(final BlueprintRunSnapshot snapshot) {
        this.blueprint = snapshot.blueprint();
        this.pickOrder = InputOrder.narrowestFirst(blueprint);
        this.totalRuns = snapshot.totalRuns();
        this.toDispatch = snapshot.toDispatch();
        for (ResourceAmount output : snapshot.awaited()) {
            awaited.add(output.resource(), output.amount());
        }
    }

    Blueprint blueprint() {
        return blueprint;
    }

    boolean hasRunsToDispatch() {
        return toDispatch > 0;
    }

    boolean isFinished() {
        return awaited.size() == 0;
    }

    /**
     * @return why the last attempt to hand out a run failed;
     *         {@link DispatchResult#ACCEPTED} when the last one succeeded
     */
    DispatchResult problem() {
        return problem;
    }

    /**
     * @return units of {@code resource} that runs already handed out still owe
     */
    long claimable(final ResourceKey resource) {
        final long owed = awaited.amountOf(resource) - toDispatch * blueprint.outputOf(resource);
        return Math.max(0, owed);
    }

    /**
     * @return units of {@code resource} that runs not handed out yet will give
     */
    long scheduled(final ResourceKey resource) {
        return Math.min(awaited.amountOf(resource), toDispatch * blueprint.outputOf(resource));
    }

    /**
     * Counts up to {@code amount} of {@code resource} as received.
     *
     * @return units counted, at most what {@link #claimable} allowed
     */
    long receive(final ResourceKey resource, final long amount) {
        final long counted = Math.min(amount, claimable(resource));
        if (counted > 0) {
            awaited.remove(resource, counted);
        }
        return counted;
    }

    /**
     * Hands one run to the first executor that takes it, starting after the
     * one that took the last run, and takes its inputs out of {@code held}.
     * Nothing happens while {@code held} lacks an input.
     *
     * @return whether a run was handed out
     */
    boolean dispatch(final ResourceCounter held, final BlueprintProvider blueprints) {
        final List<ResourceAmount> inputs = pickInputs(held);
        if (inputs.isEmpty()) {
            return false;
        }
        final List<BlueprintExecutor> executors = blueprints.executorsFor(blueprint);
        if (executors.isEmpty()) {
            problem = DispatchResult.NO_TARGET;
            return false;
        }
        for (int tried = 0; tried < executors.size(); tried++) {
            final int index = (nextExecutor + tried) % executors.size();
            final DispatchResult result = tryExecutor(executors.get(index), inputs);
            problem = result;
            if (result.isAccepted()) {
                takeInputs(held, inputs);
                nextExecutor = index + 1;
                toDispatch--;
                return true;
            }
        }
        return false;
    }

    private DispatchResult tryExecutor(final BlueprintExecutor executor, final List<ResourceAmount> inputs) {
        final DispatchResult simulated = executor.dispatch(blueprint, inputs, Action.SIMULATE);
        return simulated.isAccepted() ? executor.dispatch(blueprint, inputs, Action.EXECUTE) : simulated;
    }

    /**
     * @return the resources one run takes out of {@code held}, each once;
     *         empty when {@code held} lacks some input
     */
    private List<ResourceAmount> pickInputs(final ResourceCounter held) {
        final ResourceCounter picked = new ResourceCounter();
        for (BlueprintInput input : pickOrder) {
            long lacking = input.amount();
            for (int i = 0; i < input.options().size() && lacking > 0; i++) {
                final ResourceKey option = input.options().get(i);
                final long taken = Math.min(lacking, held.amountOf(option) - picked.amountOf(option));
                if (taken > 0) {
                    picked.add(option, taken);
                    lacking -= taken;
                }
            }
            if (lacking > 0) {
                return List.of();
            }
        }
        return picked.contents();
    }

    private static void takeInputs(final ResourceCounter held, final List<ResourceAmount> inputs) {
        for (ResourceAmount input : inputs) {
            held.remove(input.resource(), input.amount());
        }
    }

    /**
     * @return units of every output all runs give together
     */
    long expectedTotal() {
        long total = 0;
        for (ResourceAmount output : blueprint.outputs()) {
            total += output.amount() * totalRuns;
        }
        return total;
    }

    long receivedTotal() {
        return expectedTotal() - awaited.total();
    }

    BlueprintRunSnapshot snapshot() {
        return new BlueprintRunSnapshot(blueprint, totalRuns, toDispatch, awaited.contents());
    }
}
