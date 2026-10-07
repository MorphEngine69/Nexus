package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.storage.NetworkStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.automation.Recipes.INGOT;
import static com.morphengine.nexus.automation.Recipes.ORE;
import static com.morphengine.nexus.automation.Recipes.SMELTING;
import static com.morphengine.nexus.automation.Recipes.amount;
import static com.morphengine.nexus.automation.Recipes.deliver;
import static com.morphengine.nexus.automation.Recipes.network;
import static org.assertj.core.api.Assertions.assertThat;

class TaskListTest {

    private static final int MANY = 64;

    private final BlueprintRegistry registry = new BlueprintRegistry();
    private final TaskList tasks = new TaskList();

    @Test
    void anOutputReachingTheNetworkCountsForOneTaskOnly() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask first = smelt(network, 1);
        final CraftingTask second = smelt(network, 1);
        network.addInterceptor(tasks);
        runAll(network);

        deliver(network, INGOT, 1);

        assertThat(first.state()).isEqualTo(TaskState.RETURNING);
        assertThat(second.state()).isEqualTo(TaskState.RUNNING);
        assertThat(tasks.awaited(INGOT)).isEqualTo(1);
    }

    @Test
    void aTaskLeavesTheListOnceDone() {
        final NetworkStorage network = network(amount(ORE, 10));
        smelt(network, 1);
        network.addInterceptor(tasks);
        runAll(network);
        deliver(network, INGOT, 1);

        tasks.step(network, registry, MANY);

        assertThat(tasks.isEmpty()).isTrue();
    }

    @Test
    void abandoningHandsOverWhatTheTasksHeld() {
        final NetworkStorage network = network(amount(ORE, 10));
        smelt(network, 3);
        tasks.step(network, registry, MANY);

        assertThat(tasks.abandon()).containsExactly(amount(ORE, 3));
        assertThat(tasks.isEmpty()).isTrue();
    }

    @Test
    void knowsWhatIsBeingCrafted() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask task = smelt(network, 1);

        assertThat(tasks.isCrafting(INGOT)).isTrue();
        tasks.cancel(task.id());
        assertThat(tasks.isCrafting(INGOT)).isFalse();
    }

    @Test
    void restoredTasksKeepTheirOrderAndIds() {
        final NetworkStorage network = network(amount(ORE, 10));
        final CraftingTask first = smelt(network, 1);
        final CraftingTask second = smelt(network, 2);
        final TaskList restored = new TaskList();

        restored.restore(tasks.snapshots());

        assertThat(restored.statuses()).extracting(status -> status.id()).containsExactly(first.id(), second.id());
    }

    private CraftingTask smelt(final NetworkStorage network, final long ingots) {
        registry.offer(new FakeExecutor(), List.of(SMELTING), 0);
        final CraftingTask task = CraftingTask.start(new CraftingPlanner(registry, network).plan(INGOT, ingots), "");
        tasks.add(task);
        return task;
    }

    private void runAll(final NetworkStorage network) {
        tasks.step(network, registry, MANY);
        tasks.step(network, registry, MANY);
    }
}
