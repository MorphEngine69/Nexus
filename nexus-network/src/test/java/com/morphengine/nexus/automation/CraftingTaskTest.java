package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.automation.TaskEntry;
import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.storage.NetworkStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.automation.Recipes.INGOT;
import static com.morphengine.nexus.automation.Recipes.LOG;
import static com.morphengine.nexus.automation.Recipes.ORE;
import static com.morphengine.nexus.automation.Recipes.PLANKS;
import static com.morphengine.nexus.automation.Recipes.PLANKS_FROM_LOG;
import static com.morphengine.nexus.automation.Recipes.SMELTING;
import static com.morphengine.nexus.automation.Recipes.SPRUCE_PLANKS;
import static com.morphengine.nexus.automation.Recipes.STICK;
import static com.morphengine.nexus.automation.Recipes.STICKS_FROM_ANY_PLANKS;
import static com.morphengine.nexus.automation.Recipes.STICKS_FROM_PLANKS;
import static com.morphengine.nexus.automation.Recipes.amount;
import static com.morphengine.nexus.automation.Recipes.deliver;
import static com.morphengine.nexus.automation.Recipes.network;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CraftingTaskTest {

    private static final int MANY = 64;

    private final BlueprintRegistry registry = new BlueprintRegistry();
    private final FakeExecutor furnace = new FakeExecutor();

    @Test
    void takesWhatThePlanUsesOutOfStorageFirst() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 3);

        task.step(network, registry, MANY);

        assertThat(task.state()).isEqualTo(TaskState.RUNNING);
        assertThat(network.amountOf(ORE)).isEqualTo(7);
        assertThat(task.holdings()).containsExactly(amount(ORE, 3));
    }

    @Test
    void waitsWhileStorageLacksWhatThePlanCountedOn() {
        final NetworkStorage network = network(amount(ORE, 3));
        final CraftingTask task = smelt(network, 3);
        network.extract(ORE, 2, Action.EXECUTE, Actor.NOBODY);

        task.step(network, registry, MANY);

        assertThat(task.state()).isEqualTo(TaskState.GATHERING);
        deliver(network, ORE, 2);
        task.step(network, registry, MANY);
        assertThat(task.state()).isEqualTo(TaskState.RUNNING);
    }

    @Test
    void handsOutNoMoreRunsPerStepThanAllowed() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 5);
        task.step(network, registry, MANY);

        task.step(network, registry, 2);

        assertThat(furnace.taken()).hasSize(2);
        assertThat(task.holdings()).containsExactly(amount(ORE, 3));
    }

    @Test
    void theResourceAskedForGoesIntoTheNetworkAndIsCounted() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = running(network, smelt(network, 2));
        network.addInterceptor(task);

        deliver(network, INGOT, 2);

        assertThat(network.amountOf(INGOT)).isEqualTo(2);
        assertThat(task.state()).isEqualTo(TaskState.RETURNING);
    }

    @Test
    void outputsOfRunsNotHandedOutYetAreNotCounted() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 2);
        network.addInterceptor(task);
        task.step(network, registry, MANY);
        task.step(network, registry, 1);

        deliver(network, INGOT, 2);

        assertThat(task.status().progress()).isEqualTo(0.5);
        assertThat(task.state()).isEqualTo(TaskState.RUNNING);
    }

    @Test
    void intermediateOutputsAreClaimedAndFeedTheNextRuns() {
        final NetworkStorage network = network(amount(LOG, 1));
        registry.offer(furnace, List.of(PLANKS_FROM_LOG, STICKS_FROM_PLANKS), 0);
        final CraftingTask task = CraftingTask.start(planner(network).plan(STICK, 4), "Steve");
        network.addInterceptor(task);
        task.step(network, registry, MANY);
        task.step(network, registry, MANY);

        deliver(network, PLANKS, 4);
        task.step(network, registry, MANY);

        assertThat(network.amountOf(PLANKS)).isZero();
        assertThat(furnace.taken()).containsExactly(PLANKS_FROM_LOG, STICKS_FROM_PLANKS);
        assertThat(task.holdings()).containsExactly(amount(PLANKS, 2));
    }

    @Test
    void whatIsLeftOverGoesBackToTheNetworkAtTheEnd() {
        final NetworkStorage network = network(amount(LOG, 1));
        registry.offer(furnace, List.of(PLANKS_FROM_LOG, STICKS_FROM_PLANKS), 0);
        final CraftingTask task = CraftingTask.start(planner(network).plan(STICK, 4), "Steve");
        network.addInterceptor(task);
        task.step(network, registry, MANY);
        task.step(network, registry, MANY);
        deliver(network, PLANKS, 4);
        task.step(network, registry, MANY);
        deliver(network, STICK, 4);

        task.step(network, registry, MANY);

        assertThat(task.isDone()).isTrue();
        assertThat(network.amountOf(PLANKS)).isEqualTo(2);
        assertThat(network.amountOf(STICK)).isEqualTo(4);
    }

    @Test
    void twoTasksOfTheSameBlueprintNeverTakeEachOthersOutputs() {
        final NetworkStorage network = network(amount(LOG, 2));
        registry.offer(furnace, List.of(PLANKS_FROM_LOG, STICKS_FROM_PLANKS), 0);
        final CraftingTask first = CraftingTask.start(planner(network).plan(STICK, 4), "Steve");
        first.step(network, registry, MANY);
        final CraftingTask second = CraftingTask.start(planner(network).plan(STICK, 4), "Alex");
        network.addInterceptor(first);
        network.addInterceptor(second);
        second.step(network, registry, MANY);
        second.step(network, registry, MANY);

        deliver(network, PLANKS, 4);

        assertThat(first.holdings()).containsExactly(amount(LOG, 1));
        assertThat(second.holdings()).containsExactly(amount(PLANKS, 4));
    }

    @Test
    void cancellingPutsBackEverythingHeld() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 4);
        task.step(network, registry, MANY);

        task.cancel();
        task.step(network, registry, MANY);

        assertThat(task.isDone()).isTrue();
        assertThat(network.amountOf(ORE)).isEqualTo(10);
    }

    @Test
    void aCancelledTaskClaimsNothing() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = running(network, smelt(network, 2));
        network.addInterceptor(task);

        task.cancel();

        assertThat(task.intercept(INGOT, 2, Action.SIMULATE)).isZero();
        assertThat(task.awaited(INGOT)).isZero();
    }

    @Test
    void anExecutorThatRefusesShowsInTheStatus() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 2);
        furnace.answer(DispatchResult.TARGET_FULL);
        task.step(network, registry, MANY);

        task.step(network, registry, MANY);

        final TaskEntry ingots = task.status().entries().stream()
                .filter(entry -> entry.resource().equals(INGOT)).findFirst().orElseThrow();
        assertThat(ingots.problem()).isEqualTo(DispatchResult.TARGET_FULL);
        assertThat(ingots.scheduled()).isEqualTo(2);
    }

    @Test
    void runsGoToTheExecutorsInTurn() {
        final FakeExecutor secondFurnace = new FakeExecutor();
        final NetworkStorage network = network(amount(ORE, 10));
        registry.offer(secondFurnace, List.of(SMELTING), 0);
        final CraftingTask task = smelt(network, 4);
        task.step(network, registry, MANY);

        task.step(network, registry, MANY);

        assertThat(furnace.taken()).hasSize(2);
        assertThat(secondFurnace.taken()).hasSize(2);
    }

    @Test
    void aRestoredTaskCarriesOnWhereItStopped() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 3);
        task.step(network, registry, MANY);
        task.step(network, registry, 2);

        final CraftingTask restored = CraftingTask.restore(task.snapshot());
        network.addInterceptor(restored);
        deliver(network, INGOT, 2);

        assertThat(restored.id()).isEqualTo(task.id());
        assertThat(restored.awaited(INGOT)).isZero();
        assertThat(restored.holdings()).containsExactly(amount(ORE, 1));
        restored.step(network, registry, MANY);
        assertThat(restored.awaited(INGOT)).isEqualTo(1);
    }

    @Test
    void anIncompletePlanCannotStart() {
        final NetworkStorage network = network();
        registry.offer(furnace, List.of(SMELTING), 0);
        final CraftingPlan plan = planner(network).plan(INGOT, 3);

        assertThatThrownBy(() -> CraftingTask.start(plan, "Steve")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRunTakesWhateverSubstituteTheTaskHolds() {
        final NetworkStorage network = network(amount(SPRUCE_PLANKS, 2));
        registry.offer(furnace, List.of(STICKS_FROM_ANY_PLANKS), 0);
        final CraftingTask task = CraftingTask.start(planner(network).plan(STICK, 4), "Steve");

        running(network, task);

        assertThat(furnace.takenInputs()).containsExactly(List.of(amount(SPRUCE_PLANKS, 2)));
    }

    @Test
    void anExactInputPicksBeforeOneWithSubstitutes() {
        final Blueprint sticks = new Blueprint(BlueprintKind.CRAFTING, List.of(
                new BlueprintInput(List.of(PLANKS, SPRUCE_PLANKS), 1), new BlueprintInput(List.of(PLANKS), 1)),
                List.of(amount(STICK, 4)));
        final NetworkStorage network = network(amount(PLANKS, 1), amount(SPRUCE_PLANKS, 1));
        registry.offer(furnace, List.of(sticks), 0);
        final CraftingTask task = CraftingTask.start(planner(network).plan(STICK, 4), "Steve");

        running(network, task);

        assertThat(furnace.takenInputs()).hasSize(1);
        assertThat(furnace.takenInputs().getFirst())
                .containsExactlyInAnyOrder(amount(PLANKS, 1), amount(SPRUCE_PLANKS, 1));
    }

    private CraftingTask smelt(final NetworkStorage network, final long ingots) {
        registry.offer(furnace, List.of(SMELTING), 0);
        return CraftingTask.start(planner(network).plan(INGOT, ingots), "Steve");
    }

    /**
     * @return {@code task} after it gathered and handed out every run it could
     */
    private CraftingTask running(final NetworkStorage network, final CraftingTask task) {
        task.step(network, registry, MANY);
        task.step(network, registry, MANY);
        return task;
    }

    private CraftingPlanner planner(final NetworkStorage network) {
        return new CraftingPlanner(registry, network);
    }
}
