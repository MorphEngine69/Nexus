package com.morphengine.nexus.api.machine;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineRecipeTest {

    private static final ResourceType TYPE = new ResourceType() {
    };
    private static final ResourceKey ORE = new Key("ore");
    private static final ResourceKey DUST = new Key("dust");
    private static final ResourceKey SAND = new Key("sand");

    @Test
    void aRecipeKeepsWhatItWasGiven() {
        final MachineRecipe recipe = MachineRecipe.of(new ResourceAmount(ORE, 1), new ResourceAmount(DUST, 2), 100, 20);

        assertThat(recipe.inputs()).containsExactly(new ResourceAmount(ORE, 1));
        assertThat(recipe.output()).isEqualTo(new ResourceAmount(DUST, 2));
        assertThat(recipe.ticks()).isEqualTo(100);
        assertThat(recipe.energyPerTick()).isEqualTo(20);
    }

    @Test
    void theInputsAreCopiedSoLaterChangesDoNotReachTheRecipe() {
        final List<ResourceAmount> inputs = new ArrayList<>(List.of(new ResourceAmount(ORE, 1)));
        final MachineRecipe recipe = new MachineRecipe(inputs, new ResourceAmount(DUST, 2), 100, 20);

        inputs.add(new ResourceAmount(SAND, 1));

        assertThat(recipe.inputs()).hasSize(1);
    }

    @Test
    void aRecipeWithoutInputsIsRefused() {
        assertThatThrownBy(() -> new MachineRecipe(List.of(), new ResourceAmount(DUST, 1), 100, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aResourceListedTwiceAmongTheInputsIsRefused() {
        final List<ResourceAmount> twice = List.of(new ResourceAmount(ORE, 1), new ResourceAmount(ORE, 2));

        assertThatThrownBy(() -> new MachineRecipe(twice, new ResourceAmount(DUST, 1), 100, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroTicksAndZeroEnergyAreRefused() {
        final ResourceAmount input = new ResourceAmount(ORE, 1);
        final ResourceAmount output = new ResourceAmount(DUST, 1);

        assertThatThrownBy(() -> MachineRecipe.of(input, output, 0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MachineRecipe.of(input, output, 100, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private record Key(String name) implements ResourceKey {

        @Override
        public ResourceType type() {
            return TYPE;
        }
    }
}
