package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.StorageView;
import com.morphengine.nexus.storage.StorageSnapshot;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Works out crafting plans off the thread that owns the network, so a large recipe tree never holds up a tick.
 *
 * <p>The network's blueprints and storage are copied when a plan is asked for, on the caller's thread; the plan is
 * made from the copies, so what changes meanwhile is not seen, and a task started from the plan waits for whatever
 * is no longer there. Cancelling the returned future stops the work at the next ingredient it would look at.
 */
public final class AsyncCraftingPlanner {

    private final Executor executor;

    /**
     * @param executor runs the planning; may be any executor, one that runs the work in the calling thread included
     */
    public AsyncCraftingPlanner(final Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    /**
     * Starts planning {@code amount} of {@code resource}. Reads the registry and the storage before it returns, so
     * the caller must be the thread that owns them.
     *
     * @param amount units asked for, must be positive
     * @return the plan once made, or exceptionally with what went wrong; cancelled when the caller cancels it
     */
    public CompletableFuture<CraftingPlan> plan(
            final BlueprintRegistry registry, final StorageView storage, final ResourceKey resource,
            final long amount, final PlanGoal goal) {
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(goal, "goal must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount of " + resource + " must be positive: " + amount);
        }
        final StorageView stored = StorageSnapshot.of(storage);
        final BlueprintProvider blueprints = registry.snapshot();
        final CompletableFuture<CraftingPlan> result = new CompletableFuture<>();
        executor.execute(() -> {
            if (result.isCancelled()) {
                return;
            }
            try {
                final CraftingPlanner planner = new CraftingPlanner(blueprints, stored, result::isCancelled);
                result.complete(goal == PlanGoal.LARGEST
                        ? planner.planLargest(resource, amount)
                        : planner.plan(resource, amount));
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }
}
