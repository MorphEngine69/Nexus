package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.automation.PlannedRuns;
import com.morphengine.nexus.api.automation.TaskEntry;
import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.storage.InsertInterceptor;
import com.morphengine.nexus.storage.ResourceCounter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A crafting plan being carried out.
 *
 * <p>First the task takes everything the plan uses from the network's storage
 * and holds it apart, where nothing else can take it. Then it hands runs to
 * executors as soon as it holds their inputs, and claims the outputs of the
 * runs it handed out as they are inserted into the network, holding them for
 * the runs that need them next. The resource asked for is never claimed: it
 * goes into the network like anything else, and the task only counts it. Once
 * every output has arrived, or the task is cancelled, it puts back whatever
 * it still holds.
 *
 * <p>The task keeps no time of its own: it moves on only when {@link #step}
 * is called, so it waits without losing anything while its owner does not
 * call it, such as a network without energy. Server thread only.
 */
public final class CraftingTask implements InsertInterceptor {

    private final UUID id;
    private final ResourceAmount target;
    private final String requester;
    private final Actor actor;
    private final ResourceCounter held = new ResourceCounter();
    private final ResourceCounter toGather = new ResourceCounter();
    private final List<BlueprintRun> runs = new ArrayList<>();
    private TaskState state;

    private CraftingTask(final UUID id, final ResourceAmount target, final String requester, final TaskState state) {
        this.id = id;
        this.target = target;
        this.requester = requester;
        this.actor = () -> requester;
        this.state = state;
    }

    /**
     * @param requester name of whoever asked for it; empty when nobody in particular did
     * @throws IllegalArgumentException if the plan is not {@linkplain CraftingPlan#isComplete complete}
     */
    public static CraftingTask start(final CraftingPlan plan, final String requester) {
        Objects.requireNonNull(requester, "requester must not be null");
        if (!plan.isComplete()) {
            throw new IllegalArgumentException("cannot start an incomplete plan for " + plan.target()
                    + ", missing " + plan.missing());
        }
        final CraftingTask task = new CraftingTask(UUID.randomUUID(), plan.target(), requester, TaskState.GATHERING);
        for (ResourceAmount used : plan.fromStorage()) {
            task.toGather.add(used.resource(), used.amount());
        }
        for (PlannedRuns planned : plan.runs()) {
            task.runs.add(new BlueprintRun(planned.blueprint(), planned.runs()));
        }
        return task;
    }

    public static CraftingTask restore(final CraftingTaskSnapshot snapshot) {
        final CraftingTask task = new CraftingTask(snapshot.id(), snapshot.target(), snapshot.requester(),
                snapshot.state());
        for (ResourceAmount amount : snapshot.held()) {
            task.held.add(amount.resource(), amount.amount());
        }
        for (ResourceAmount amount : snapshot.toGather()) {
            task.toGather.add(amount.resource(), amount.amount());
        }
        for (BlueprintRunSnapshot run : snapshot.runs()) {
            task.runs.add(new BlueprintRun(run));
        }
        return task;
    }

    public UUID id() {
        return id;
    }

    public ResourceAmount target() {
        return target;
    }

    public TaskState state() {
        return state;
    }

    public boolean isDone() {
        return state == TaskState.DONE;
    }

    /**
     * Moves the task on: gathers, hands out up to {@code dispatches} runs, or
     * puts back what it holds, depending on where it is.
     *
     * @param network    the network's storage, taken from and put back into
     * @param blueprints where the executors of each blueprint are found
     * @param dispatches most runs to hand out in this step
     * @return whether anything changed
     */
    public boolean step(final Storage network, final BlueprintProvider blueprints, final int dispatches) {
        return switch (state) {
            case GATHERING -> gather(network);
            case RUNNING -> dispatch(blueprints, dispatches);
            case RETURNING -> giveBack(network);
            case PAUSED, DONE -> false;
        };
    }

    /**
     * Stops handing out runs and claiming outputs; what the task holds goes
     * back to the network on the next steps.
     */
    public void cancel() {
        if (state != TaskState.DONE) {
            state = TaskState.RETURNING;
            runs.clear();
            toGather.clear();
        }
    }

    /**
     * Claims outputs of runs this task handed out, except the resource asked
     * for, which only {@link #inserted} counts.
     */
    @Override
    public long intercept(final ResourceKey resource, final long amount, final Action action) {
        if (state != TaskState.RUNNING || resource.equals(target.resource())) {
            return 0;
        }
        long claimed = 0;
        for (int i = 0; i < runs.size() && claimed < amount; i++) {
            final BlueprintRun run = runs.get(i);
            claimed += action.isExecute() ? run.receive(resource, amount - claimed)
                    : Math.min(amount - claimed, run.claimable(resource));
        }
        if (claimed > 0 && action.isExecute()) {
            held.add(resource, claimed);
            finishIfReceivedAll();
        }
        return claimed;
    }

    /**
     * Counts the resource asked for as it reaches the network.
     */
    @Override
    public long inserted(final ResourceKey resource, final long amount) {
        if (state != TaskState.RUNNING || !resource.equals(target.resource())) {
            return 0;
        }
        long counted = 0;
        for (int i = 0; i < runs.size() && counted < amount; i++) {
            counted += runs.get(i).receive(resource, amount - counted);
        }
        finishIfReceivedAll();
        return counted;
    }

    /**
     * @return units of {@code resource} that runs this task handed out still owe it
     */
    public long awaited(final ResourceKey resource) {
        if (state != TaskState.RUNNING) {
            return 0;
        }
        long owed = 0;
        for (BlueprintRun run : runs) {
            owed += run.claimable(resource);
        }
        return owed;
    }

    /**
     * @return what the task holds now, gathered or crafted
     */
    public List<ResourceAmount> holdings() {
        return held.contents();
    }

    public TaskStatus status() {
        final Set<ResourceKey> resources = new LinkedHashSet<>();
        for (ResourceAmount amount : held.contents()) {
            resources.add(amount.resource());
        }
        for (BlueprintRun run : runs) {
            for (ResourceAmount output : run.blueprint().outputs()) {
                resources.add(output.resource());
            }
        }
        final List<TaskEntry> entries = new ArrayList<>(resources.size());
        for (ResourceKey resource : resources) {
            entries.add(entryOf(resource));
        }
        return new TaskStatus(id, target, requester, state, progress(), entries);
    }

    public CraftingTaskSnapshot snapshot() {
        final List<BlueprintRunSnapshot> saved = new ArrayList<>(runs.size());
        for (BlueprintRun run : runs) {
            saved.add(run.snapshot());
        }
        return new CraftingTaskSnapshot(id, target, requester, state, held.contents(), toGather.contents(), saved);
    }

    private boolean gather(final Storage network) {
        boolean changed = false;
        for (ResourceAmount needed : toGather.contents()) {
            final long taken = network.extract(needed.resource(), needed.amount(), Action.EXECUTE, actor);
            if (taken > 0) {
                toGather.remove(needed.resource(), taken);
                held.add(needed.resource(), taken);
                changed = true;
            }
        }
        if (toGather.size() == 0) {
            state = TaskState.RUNNING;
            changed = true;
        }
        return changed;
    }

    private boolean dispatch(final BlueprintProvider blueprints, final int dispatches) {
        int left = dispatches;
        for (int i = 0; i < runs.size() && left > 0; i++) {
            final BlueprintRun run = runs.get(i);
            while (left > 0 && run.hasRunsToDispatch() && run.dispatch(held, blueprints)) {
                left--;
            }
        }
        return left < dispatches;
    }

    private void finishIfReceivedAll() {
        for (BlueprintRun run : runs) {
            if (!run.isFinished()) {
                return;
            }
        }
        state = TaskState.RETURNING;
    }

    private boolean giveBack(final Storage network) {
        boolean changed = false;
        for (ResourceAmount kept : held.contents()) {
            final long inserted = network.insert(kept.resource(), kept.amount(), Action.EXECUTE, actor);
            if (inserted > 0) {
                held.remove(kept.resource(), inserted);
                changed = true;
            }
        }
        if (held.size() == 0) {
            state = TaskState.DONE;
            changed = true;
        }
        return changed;
    }

    private TaskEntry entryOf(final ResourceKey resource) {
        long scheduled = 0;
        long processing = 0;
        DispatchResult problem = DispatchResult.ACCEPTED;
        for (BlueprintRun run : runs) {
            scheduled += run.scheduled(resource);
            processing += run.claimable(resource);
            if (run.hasRunsToDispatch() && run.blueprint().outputOf(resource) > 0
                    && problem.isAccepted()) {
                problem = run.problem();
            }
        }
        return new TaskEntry(resource, held.amountOf(resource), scheduled, processing, problem);
    }

    /**
     * @return share of all outputs received; everything once the task is done
     */
    private double progress() {
        if (state == TaskState.DONE) {
            return 1;
        }
        long expected = 0;
        long received = 0;
        for (BlueprintRun run : runs) {
            expected += run.expectedTotal();
            received += run.receivedTotal();
        }
        return expected == 0 ? 0 : (double) received / expected;
    }
}
