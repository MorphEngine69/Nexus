package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.storage.InsertInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The crafting tasks one owner keeps, in the order they were started. The
 * earlier task claims an output first. A task leaves the list once it is done.
 * Server thread only.
 */
public final class TaskList implements InsertInterceptor {

    private final List<CraftingTask> tasks = new ArrayList<>();

    public void add(final CraftingTask task) {
        tasks.add(task);
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Moves every task on, the earlier first, sharing {@code dispatches} runs
     * between them, and drops those that are done.
     *
     * @return whether anything changed
     */
    public boolean step(final Storage network, final BlueprintProvider blueprints, final int dispatches) {
        boolean changed = false;
        for (int i = 0; i < tasks.size(); i++) {
            changed |= tasks.get(i).step(network, blueprints, dispatches);
        }
        return tasks.removeIf(CraftingTask::isDone) || changed;
    }

    /**
     * @return whether a task of that id was found and cancelled
     */
    public boolean cancel(final UUID id) {
        for (CraftingTask task : tasks) {
            if (task.id().equals(id)) {
                task.cancel();
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels every task and hands over what they hold, for when their owner
     * goes away; the list is empty afterwards.
     *
     * @return everything the tasks held
     */
    public List<ResourceAmount> abandon() {
        final List<ResourceAmount> held = new ArrayList<>();
        for (CraftingTask task : tasks) {
            task.cancel();
            held.addAll(task.holdings());
        }
        tasks.clear();
        return held;
    }

    public List<TaskStatus> statuses() {
        final List<TaskStatus> statuses = new ArrayList<>(tasks.size());
        for (CraftingTask task : tasks) {
            statuses.add(task.status());
        }
        return statuses;
    }

    /**
     * @return units of {@code resource} the tasks' handed out runs still owe them
     */
    public long awaited(final ResourceKey resource) {
        long owed = 0;
        for (int i = 0; i < tasks.size(); i++) {
            owed += tasks.get(i).awaited(resource);
        }
        return owed;
    }

    /**
     * @return whether a task that is not done or cancelled is crafting {@code resource}
     */
    public boolean isCrafting(final ResourceKey resource) {
        for (CraftingTask task : tasks) {
            if (task.target().resource().equals(resource) && task.state() != TaskState.DONE
                    && task.state() != TaskState.RETURNING) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long intercept(final ResourceKey resource, final long amount, final Action action) {
        long claimed = 0;
        for (int i = 0; i < tasks.size() && claimed < amount; i++) {
            claimed += tasks.get(i).intercept(resource, amount - claimed, action);
        }
        return claimed;
    }

    @Override
    public long inserted(final ResourceKey resource, final long amount) {
        long counted = 0;
        for (int i = 0; i < tasks.size() && counted < amount; i++) {
            counted += tasks.get(i).inserted(resource, amount - counted);
        }
        return counted;
    }

    public List<CraftingTaskSnapshot> snapshots() {
        final List<CraftingTaskSnapshot> snapshots = new ArrayList<>(tasks.size());
        for (CraftingTask task : tasks) {
            snapshots.add(task.snapshot());
        }
        return snapshots;
    }

    /**
     * Replaces the tasks with those saved in {@code snapshots}.
     */
    public void restore(final List<CraftingTaskSnapshot> snapshots) {
        tasks.clear();
        for (CraftingTaskSnapshot snapshot : snapshots) {
            tasks.add(CraftingTask.restore(snapshot));
        }
    }
}
