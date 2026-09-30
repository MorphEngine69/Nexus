package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlueprintTest {

    private static final ResourceType ITEMS = new ResourceType() {
    };
    private static final ResourceKey PLANKS = new Key("planks");
    private static final ResourceKey STICK = new Key("stick");
    private static final ResourceKey COAL = new Key("coal");
    private static final ResourceKey SPRUCE_PLANKS = new Key("spruce_planks");

    @Test
    void mergesTheSameInputGivenInSeveralSlots() {
        final Blueprint blueprint = Blueprint.exact(BlueprintKind.CRAFTING,
                List.of(new ResourceAmount(PLANKS, 1), new ResourceAmount(PLANKS, 1)),
                List.of(new ResourceAmount(STICK, 4)));

        assertThat(blueprint.inputs()).containsExactly(new BlueprintInput(List.of(PLANKS), 2));
    }

    @Test
    void blueprintsWithTheSameContentsAreEqualHoweverTheSlotsWereSplit() {
        final Blueprint split = Blueprint.exact(BlueprintKind.CRAFTING,
                List.of(new ResourceAmount(PLANKS, 1), new ResourceAmount(PLANKS, 1)),
                List.of(new ResourceAmount(STICK, 4)));
        final Blueprint whole = Blueprint.exact(BlueprintKind.CRAFTING,
                List.of(new ResourceAmount(PLANKS, 2)), List.of(new ResourceAmount(STICK, 4)));

        assertThat(split).isEqualTo(whole).hasSameHashCodeAs(whole);
    }

    @Test
    void theFirstOutputIsTheMainProduct() {
        final Blueprint blueprint = Blueprint.exact(BlueprintKind.PROCESSING,
                List.of(new ResourceAmount(PLANKS, 1)),
                List.of(new ResourceAmount(COAL, 1), new ResourceAmount(STICK, 2)));

        assertThat(blueprint.primaryOutput()).isEqualTo(new ResourceAmount(COAL, 1));
        assertThat(blueprint.outputOf(STICK)).isEqualTo(2);
        assertThat(blueprint.outputOf(PLANKS)).isZero();
    }

    @Test
    void rejectsABlueprintWithoutOutputs() {
        assertThatThrownBy(() -> Blueprint.exact(BlueprintKind.PROCESSING,
                List.of(new ResourceAmount(PLANKS, 1)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlueprintWithoutInputs() {
        assertThatThrownBy(() -> Blueprint.exact(BlueprintKind.CRAFTING,
                List.of(), List.of(new ResourceAmount(STICK, 4))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInputsWhoseSumOverflows() {
        assertThatThrownBy(() -> Blueprint.exact(BlueprintKind.PROCESSING,
                List.of(new ResourceAmount(PLANKS, Long.MAX_VALUE), new ResourceAmount(PLANKS, 1)),
                List.of(new ResourceAmount(STICK, 1))))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void aPlanWithAnythingMissingCannotStart() {
        final Blueprint blueprint = Blueprint.exact(BlueprintKind.CRAFTING,
                List.of(new ResourceAmount(PLANKS, 2)), List.of(new ResourceAmount(STICK, 4)));
        final CraftingPlan plan = new CraftingPlan(new ResourceAmount(STICK, 4),
                List.of(new PlannedRuns(blueprint, 1)), List.of(), List.of(new ResourceAmount(STICK, 4)),
                List.of(new ResourceAmount(PLANKS, 2)));

        assertThat(plan.isComplete()).isFalse();
    }

    @Test
    void mergesInputsAcceptingTheSameResources() {
        final BlueprintInput anyPlanks = new BlueprintInput(List.of(PLANKS, SPRUCE_PLANKS), 1);
        final Blueprint blueprint = new Blueprint(BlueprintKind.CRAFTING, List.of(anyPlanks, anyPlanks),
                List.of(new ResourceAmount(STICK, 4)));

        assertThat(blueprint.inputs()).containsExactly(new BlueprintInput(List.of(PLANKS, SPRUCE_PLANKS), 2));
    }

    @Test
    void keepsAnExactInputApartFromOneWithSubstitutes() {
        final Blueprint blueprint = new Blueprint(BlueprintKind.CRAFTING,
                List.of(new BlueprintInput(List.of(PLANKS, SPRUCE_PLANKS), 1), new BlueprintInput(List.of(PLANKS), 1)),
                List.of(new ResourceAmount(STICK, 4)));

        assertThat(blueprint.inputs()).hasSize(2);
    }

    @Test
    void anInputPrefersTheResourceItWasEncodedWith() {
        final BlueprintInput input = new BlueprintInput(List.of(SPRUCE_PLANKS, PLANKS), 1);

        assertThat(input.preferred()).isEqualTo(SPRUCE_PLANKS);
        assertThat(input.accepts(PLANKS)).isTrue();
        assertThat(input.accepts(STICK)).isFalse();
        assertThat(input.hasSubstitutes()).isTrue();
    }

    @Test
    void rejectsAnInputWithoutOptions() {
        assertThatThrownBy(() -> new BlueprintInput(List.of(), 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnInputListingAnOptionTwice() {
        assertThatThrownBy(() -> new BlueprintInput(List.of(PLANKS, PLANKS), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnInputOfNoUnits() {
        assertThatThrownBy(() -> new BlueprintInput(List.of(PLANKS), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private record Key(String name) implements ResourceKey {

        @Override
        public ResourceType type() {
            return ITEMS;
        }
    }
}
