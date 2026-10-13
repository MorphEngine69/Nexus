package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.storage.NetworkStorage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import static com.morphengine.nexus.automation.Recipes.LOG;
import static com.morphengine.nexus.automation.Recipes.PLANKS;
import static com.morphengine.nexus.automation.Recipes.PLANKS_FROM_LOG;
import static com.morphengine.nexus.automation.Recipes.STICK;
import static com.morphengine.nexus.automation.Recipes.STICKS_FROM_PLANKS;
import static com.morphengine.nexus.automation.Recipes.amount;
import static com.morphengine.nexus.automation.Recipes.deliver;
import static com.morphengine.nexus.automation.Recipes.network;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncCraftingPlannerTest {

    private final BlueprintRegistry registry = new BlueprintRegistry();
    private final List<Runnable> queued = new ArrayList<>();
    private final AsyncCraftingPlanner planner = new AsyncCraftingPlanner(queued::add);

    @Test
    void makesTheSamePlanAsPlanningInPlace() {
        registry.offer(new FakeExecutor(), List.of(PLANKS_FROM_LOG, STICKS_FROM_PLANKS), 0);
        final NetworkStorage storage = network(amount(LOG, 3));

        final CompletableFuture<CraftingPlan> future = planner.plan(registry, storage, STICK, 1000, PlanGoal.LARGEST);
        runQueued();

        assertThat(future.join()).isEqualTo(new CraftingPlanner(registry, storage).planLargest(STICK, 1000));
    }

    @Test
    void exactGoalPlansTheAmountAskedForEvenWhenItCannotStart() {
        registry.offer(new FakeExecutor(), List.of(STICKS_FROM_PLANKS), 0);

        final CompletableFuture<CraftingPlan> future =
                planner.plan(registry, network(amount(PLANKS, 2)), STICK, 100, PlanGoal.EXACT);
        runQueued();

        assertThat(future.join().target()).isEqualTo(amount(STICK, 100));
        assertThat(future.join().isComplete()).isFalse();
    }

    @Test
    void doesNotPlanUntilTheExecutorRunsIt() {
        registry.offer(new FakeExecutor(), List.of(STICKS_FROM_PLANKS), 0);

        final CompletableFuture<CraftingPlan> future =
                planner.plan(registry, network(amount(PLANKS, 2)), STICK, 4, PlanGoal.EXACT);

        assertThat(future).isNotDone();
        runQueued();
        assertThat(future.join().isComplete()).isTrue();
    }

    @Test
    void plansFromWhatTheStorageHeldWhenAsked() {
        registry.offer(new FakeExecutor(), List.of(STICKS_FROM_PLANKS), 0);
        final NetworkStorage storage = network(amount(PLANKS, 2));
        final CompletableFuture<CraftingPlan> future = planner.plan(registry, storage, STICK, 8, PlanGoal.EXACT);

        deliver(storage, PLANKS, 100);
        runQueued();

        assertThat(future.join().isComplete()).isFalse();
        assertThat(future.join().missing()).containsExactly(amount(PLANKS, 2));
    }

    @Test
    void plansFromTheBlueprintsOfTheMomentItWasAsked() {
        final FakeExecutor assembler = new FakeExecutor();
        registry.offer(assembler, List.of(STICKS_FROM_PLANKS), 0);
        final CompletableFuture<CraftingPlan> future =
                planner.plan(registry, network(amount(PLANKS, 2)), STICK, 4, PlanGoal.EXACT);

        registry.withdraw(assembler);
        runQueued();

        assertThat(future.join().isComplete()).isTrue();
    }

    @Test
    void aCancelledRequestStaysCancelled() {
        registry.offer(new FakeExecutor(), List.of(STICKS_FROM_PLANKS), 0);
        final CompletableFuture<CraftingPlan> future =
                planner.plan(registry, network(amount(PLANKS, 2)), STICK, 4, PlanGoal.EXACT);

        future.cancel(true);
        runQueued();

        assertThat(future).isCancelled();
    }

    @Test
    void anExecutorThatRefusesFailsTheRequest() {
        final AsyncCraftingPlanner refusing = new AsyncCraftingPlanner(task -> {
            throw new RejectedExecutionException("shut down");
        });

        assertThatThrownBy(() -> refusing.plan(registry, network(), STICK, 4, PlanGoal.EXACT))
                .isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void anAmountThatIsNotPositiveIsRefusedAtOnce() {
        assertThatThrownBy(() -> planner.plan(registry, network(), STICK, 0, PlanGoal.EXACT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(queued).isEmpty();
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> planner.plan(null, network(), STICK, 1, PlanGoal.EXACT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> planner.plan(registry, network(), null, 1, PlanGoal.EXACT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> planner.plan(registry, network(), STICK, 1, null))
                .isInstanceOf(NullPointerException.class);
    }

    private void runQueued() {
        final List<Runnable> running = List.copyOf(queued);
        queued.clear();
        running.forEach(Runnable::run);
    }
}
