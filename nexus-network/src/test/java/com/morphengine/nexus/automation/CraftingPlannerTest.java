package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.automation.PlannedRuns;
import com.morphengine.nexus.storage.NetworkStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.automation.Recipes.COAL;
import static com.morphengine.nexus.automation.Recipes.INGOT;
import static com.morphengine.nexus.automation.Recipes.LOG;
import static com.morphengine.nexus.automation.Recipes.ORE;
import static com.morphengine.nexus.automation.Recipes.PLANKS;
import static com.morphengine.nexus.automation.Recipes.PLANKS_FROM_LOG;
import static com.morphengine.nexus.automation.Recipes.SPRUCE_LOG;
import static com.morphengine.nexus.automation.Recipes.SPRUCE_PLANKS;
import static com.morphengine.nexus.automation.Recipes.SPRUCE_PLANKS_FROM_LOG;
import static com.morphengine.nexus.automation.Recipes.STICK;
import static com.morphengine.nexus.automation.Recipes.STICKS_FROM_ANY_PLANKS;
import static com.morphengine.nexus.automation.Recipes.STICKS_FROM_PLANKS;
import static com.morphengine.nexus.automation.Recipes.TORCH;
import static com.morphengine.nexus.automation.Recipes.TORCHES;
import static com.morphengine.nexus.automation.Recipes.amount;
import static com.morphengine.nexus.automation.Recipes.crafting;
import static com.morphengine.nexus.automation.Recipes.network;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CraftingPlannerTest {

    private final BlueprintRegistry registry = new BlueprintRegistry();
    private final FakeExecutor assembler = new FakeExecutor();

    @Test
    void takesIngredientsFromStorage() {
        offer(STICKS_FROM_PLANKS);

        final CraftingPlan plan = planner(network(amount(PLANKS, 10))).plan(STICK, 8);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.runs()).containsExactly(new PlannedRuns(STICKS_FROM_PLANKS, 2));
        assertThat(plan.fromStorage()).containsExactly(amount(PLANKS, 4));
    }

    @Test
    void craftsTheTargetEvenWhenStorageHoldsIt() {
        offer(STICKS_FROM_PLANKS);

        final CraftingPlan plan = planner(network(amount(STICK, 64), amount(PLANKS, 2))).plan(STICK, 4);

        assertThat(plan.runs()).containsExactly(new PlannedRuns(STICKS_FROM_PLANKS, 1));
        assertThat(plan.fromStorage()).containsExactly(amount(PLANKS, 2));
    }

    @Test
    void craftsWhatStorageLacksWithItsOwnBlueprintFirst() {
        offer(PLANKS_FROM_LOG, STICKS_FROM_PLANKS);

        final CraftingPlan plan = planner(network(amount(LOG, 5))).plan(STICK, 8);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.runs()).containsExactly(
                new PlannedRuns(PLANKS_FROM_LOG, 1), new PlannedRuns(STICKS_FROM_PLANKS, 2));
        assertThat(plan.fromStorage()).containsExactly(amount(LOG, 1));
    }

    @Test
    void usesStoredIngredientsBeforeCraftingMore() {
        offer(PLANKS_FROM_LOG, STICKS_FROM_PLANKS);

        final CraftingPlan plan = planner(network(amount(PLANKS, 2), amount(LOG, 5))).plan(STICK, 8);

        assertThat(plan.runs()).containsExactly(
                new PlannedRuns(PLANKS_FROM_LOG, 1), new PlannedRuns(STICKS_FROM_PLANKS, 2));
        assertThat(plan.fromStorage()).containsExactlyInAnyOrder(amount(PLANKS, 2), amount(LOG, 1));
    }

    @Test
    void whatOneRunGivesBeyondItsNeedServesTheNextNeed() {
        offer(PLANKS_FROM_LOG, STICKS_FROM_PLANKS, TORCHES);

        final CraftingPlan plan = planner(network(amount(LOG, 9), amount(COAL, 9))).plan(TORCH, 12);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.runs()).contains(new PlannedRuns(STICKS_FROM_PLANKS, 1), new PlannedRuns(PLANKS_FROM_LOG, 1));
        assertThat(plan.fromStorage()).containsExactlyInAnyOrder(amount(LOG, 1), amount(COAL, 3));
    }

    @Test
    void reportsWhatNeitherStorageNorAnyBlueprintGivesAsMissing() {
        offer(STICKS_FROM_PLANKS);

        final CraftingPlan plan = planner(network(amount(PLANKS, 3))).plan(STICK, 8);

        assertThat(plan.isComplete()).isFalse();
        assertThat(plan.missing()).containsExactly(amount(PLANKS, 1));
        assertThat(plan.fromStorage()).containsExactly(amount(PLANKS, 3));
    }

    @Test
    void aResourceWithoutBlueprintIsMissingInFull() {
        final CraftingPlan plan = planner(network()).plan(STICK, 5);

        assertThat(plan.isComplete()).isFalse();
        assertThat(plan.missing()).containsExactly(amount(STICK, 5));
        assertThat(plan.runs()).isEmpty();
    }

    @Test
    void fallsBackToAnotherBlueprintWhenTheFirstLacksIngredients() {
        final Blueprint sticksFromLogs = crafting(List.of(amount(LOG, 1)), amount(STICK, 2));
        registry.offer(assembler, List.of(STICKS_FROM_PLANKS), 5);
        registry.offer(new FakeExecutor(), List.of(sticksFromLogs), 0);

        final CraftingPlan plan = planner(network(amount(LOG, 3))).plan(STICK, 4);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.runs()).containsExactly(new PlannedRuns(sticksFromLogs, 2));
    }

    @Test
    void withNoBlueprintWorkingItShowsWhatThePreferredOneLacks() {
        final Blueprint sticksFromLogs = crafting(List.of(amount(LOG, 1)), amount(STICK, 2));
        registry.offer(assembler, List.of(STICKS_FROM_PLANKS), 5);
        registry.offer(new FakeExecutor(), List.of(sticksFromLogs), 0);

        final CraftingPlan plan = planner(network()).plan(STICK, 4);

        assertThat(plan.missing()).containsExactly(amount(PLANKS, 2));
    }

    @Test
    void aBlueprintNeedingItsOwnProductCountsAsMissing() {
        final Blueprint planksFromSticks = crafting(List.of(amount(STICK, 4)), amount(PLANKS, 2));
        offer(STICKS_FROM_PLANKS, planksFromSticks);

        final CraftingPlan plan = planner(network()).plan(STICK, 4);

        assertThat(plan.isComplete()).isFalse();
        assertThat(plan.missing()).containsExactly(amount(STICK, 4));
    }

    @Test
    void anAmountTooLargeForALongIsMissing() {
        offer(STICKS_FROM_PLANKS);

        final CraftingPlan plan = planner(network()).plan(STICK, Long.MAX_VALUE);

        assertThat(plan.isComplete()).isFalse();
        assertThat(plan.missing()).containsExactly(amount(STICK, Long.MAX_VALUE));
    }

    @Test
    void processingBlueprintsArePlannedLikeCraftingOnes() {
        final Blueprint smelting = Blueprint.exact(BlueprintKind.PROCESSING, List.of(amount(ORE, 1)),
                List.of(amount(INGOT, 1)));
        offer(smelting);

        final CraftingPlan plan = planner(network(amount(ORE, 64))).plan(INGOT, 10);

        assertThat(plan.runs()).containsExactly(new PlannedRuns(smelting, 10));
        assertThat(plan.crafted()).containsExactly(amount(INGOT, 10));
    }

    @Test
    void rejectsAnAmountOfZero() {
        assertThatThrownBy(() -> planner(network()).plan(STICK, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void takesASubstituteWhenStorageLacksTheEncodedResource() {
        offer(STICKS_FROM_ANY_PLANKS);

        final CraftingPlan plan = planner(network(amount(SPRUCE_PLANKS, 10))).plan(STICK, 4);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.fromStorage()).containsExactly(amount(SPRUCE_PLANKS, 2));
    }

    @Test
    void makesUpAnInputFromSeveralSubstitutes() {
        offer(STICKS_FROM_ANY_PLANKS);

        final CraftingPlan plan = planner(network(amount(PLANKS, 1), amount(SPRUCE_PLANKS, 1))).plan(STICK, 4);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.fromStorage()).containsExactlyInAnyOrder(amount(PLANKS, 1), amount(SPRUCE_PLANKS, 1));
    }

    @Test
    void takesStoredSubstitutesBeforeCraftingTheEncodedResource() {
        offer(PLANKS_FROM_LOG, STICKS_FROM_ANY_PLANKS);

        final CraftingPlan plan = planner(network(amount(SPRUCE_PLANKS, 2), amount(LOG, 5))).plan(STICK, 4);

        assertThat(plan.runs()).containsExactly(new PlannedRuns(STICKS_FROM_ANY_PLANKS, 1));
        assertThat(plan.fromStorage()).containsExactly(amount(SPRUCE_PLANKS, 2));
    }

    @Test
    void craftsTheFirstSubstituteSomeBlueprintGives() {
        offer(SPRUCE_PLANKS_FROM_LOG, STICKS_FROM_ANY_PLANKS);

        final CraftingPlan plan = planner(network(amount(SPRUCE_LOG, 1))).plan(STICK, 4);

        assertThat(plan.isComplete()).isTrue();
        assertThat(plan.runs()).containsExactly(
                new PlannedRuns(SPRUCE_PLANKS_FROM_LOG, 1), new PlannedRuns(STICKS_FROM_ANY_PLANKS, 1));
    }

    @Test
    void reportsTheEncodedResourceMissingWhenNoSubstituteCanBeHad() {
        offer(STICKS_FROM_ANY_PLANKS);

        final CraftingPlan plan = planner(network()).plan(STICK, 4);

        assertThat(plan.missing()).containsExactly(amount(PLANKS, 2));
    }

    private void offer(final Blueprint... blueprints) {
        registry.offer(assembler, List.of(blueprints), 0);
    }

    private CraftingPlanner planner(final NetworkStorage storage) {
        return new CraftingPlanner(registry, storage);
    }
}
