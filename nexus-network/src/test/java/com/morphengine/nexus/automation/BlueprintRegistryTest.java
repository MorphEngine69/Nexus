package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.automation.Recipes.LOG;
import static com.morphengine.nexus.automation.Recipes.PLANKS;
import static com.morphengine.nexus.automation.Recipes.PLANKS_FROM_LOG;
import static com.morphengine.nexus.automation.Recipes.STICK;
import static com.morphengine.nexus.automation.Recipes.STICKS_FROM_PLANKS;
import static com.morphengine.nexus.automation.Recipes.amount;
import static com.morphengine.nexus.automation.Recipes.crafting;
import static org.assertj.core.api.Assertions.assertThat;

class BlueprintRegistryTest {

    private final BlueprintRegistry registry = new BlueprintRegistry();
    private final FakeExecutor first = new FakeExecutor();
    private final FakeExecutor second = new FakeExecutor();

    @Test
    void theExecutorOfTheHighestPriorityComesFirst() {
        registry.offer(first, List.of(STICKS_FROM_PLANKS), 0);
        registry.offer(second, List.of(STICKS_FROM_PLANKS), 3);

        assertThat(registry.executorsFor(STICKS_FROM_PLANKS)).containsExactly(second, first);
    }

    @Test
    void onATieWhoeverOfferedFirstComesFirst() {
        registry.offer(first, List.of(STICKS_FROM_PLANKS), 0);
        registry.offer(second, List.of(STICKS_FROM_PLANKS), 0);

        assertThat(registry.executorsFor(STICKS_FROM_PLANKS)).containsExactly(first, second);
    }

    @Test
    void blueprintsForAResourceComeByPriority() {
        final Blueprint sticksFromLogs = crafting(List.of(amount(LOG, 1)), amount(STICK, 2));
        registry.offer(first, List.of(STICKS_FROM_PLANKS), 0);
        registry.offer(second, List.of(sticksFromLogs), 1);

        assertThat(registry.blueprintsFor(STICK)).containsExactly(sticksFromLogs, STICKS_FROM_PLANKS);
        assertThat(registry.blueprintsFor(LOG)).isEmpty();
    }

    @Test
    void offeringAgainReplacesWhatTheExecutorOfferedBefore() {
        registry.offer(first, List.of(STICKS_FROM_PLANKS), 0);

        registry.offer(first, List.of(PLANKS_FROM_LOG), 0);

        assertThat(registry.craftables()).containsExactly(PLANKS);
        assertThat(registry.executorsFor(STICKS_FROM_PLANKS)).isEmpty();
    }

    @Test
    void aWithdrawnExecutorOffersNothing() {
        registry.offer(first, List.of(STICKS_FROM_PLANKS), 0);
        registry.offer(second, List.of(STICKS_FROM_PLANKS), 0);

        registry.withdraw(first);

        assertThat(registry.executorsFor(STICKS_FROM_PLANKS)).containsExactly(second);
    }

    @Test
    void everyChangeBumpsTheRevision() {
        final int before = registry.revision();
        registry.offer(first, List.of(STICKS_FROM_PLANKS), 0);
        final int afterOffer = registry.revision();
        registry.withdraw(first);

        assertThat(afterOffer).isGreaterThan(before);
        assertThat(registry.revision()).isGreaterThan(afterOffer);
    }

    @Test
    void withdrawingAnExecutorThatOfferedNothingChangesNothing() {
        final int before = registry.revision();

        registry.withdraw(first);

        assertThat(registry.revision()).isEqualTo(before);
    }
}
