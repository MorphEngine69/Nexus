package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.machine.MachineRecipes;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.morphengine.nexus.api.storage.Actor.NOBODY;
import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class MachineAlloyingTest {

    private static final long LIMIT = 64;
    private static final long PLENTY = 100_000;
    private static final int NORMAL = 100;
    private static final int TICKS = 4;

    private static final MachineRecipe MIX = new MachineRecipe(
            List.of(new ResourceAmount(STONE, 2), new ResourceAmount(SAND, 1)), new ResourceAmount(DIRT, 3), TICKS, 1);

    private static final MachineRecipes STONE_AND_SAND = available ->
            covers(available, STONE, 2) && covers(available, SAND, 1) ? Optional.of(MIX) : Optional.empty();

    private final MachineSlots slots = new PlainMachineSlots(resource -> LIMIT);
    private final MachineInventory inventory = new MachineInventory(1, 3, slots);
    private final MachineLine line = new MachineLine(inventory.inputsOf(0), inventory.output(0), STONE_AND_SAND);
    private final SimpleEnergyBuffer energy = new SimpleEnergyBuffer(PLENTY, PLENTY, PLENTY);

    private static boolean covers(final List<ResourceAmount> available, final ResourceKey resource, final long amount) {
        return available.stream().anyMatch(have -> have.resource().equals(resource) && have.amount() >= amount);
    }

    private void runUntilDone() {
        for (int tick = 0; tick < 100 && inventory.output(0).isEmpty(); tick++) {
            line.tick(energy, NORMAL);
        }
    }

    @Test
    void aLineWorksOnWhatItsSeveralInputSlotsHoldTogether() {
        energy.insert(PLENTY, Action.EXECUTE);
        inventory.input(0).insert(STONE, 5, Action.EXECUTE);
        inventory.input(2).insert(SAND, 2, Action.EXECUTE);

        runUntilDone();

        assertThat(inventory.output(0).contents()).contains(new ResourceAmount(DIRT, 3));
        assertThat(inventory.input(0).amount()).isEqualTo(3);
        assertThat(inventory.input(2).amount()).isEqualTo(1);
    }

    @Test
    void aLineWaitsWhileOneOfTheInputsIsMissing() {
        energy.insert(PLENTY, Action.EXECUTE);
        inventory.input(0).insert(STONE, 5, Action.EXECUTE);

        runUntilDone();

        assertThat(inventory.output(0).isEmpty()).isTrue();
        assertThat(line.isRunning()).isFalse();
    }

    @Test
    void aResourceThatIsSplitOverTwoSlotsCountsOnce() {
        energy.insert(PLENTY, Action.EXECUTE);
        inventory.input(0).insert(STONE, 1, Action.EXECUTE);
        inventory.input(1).insert(STONE, 1, Action.EXECUTE);
        inventory.input(2).insert(SAND, 1, Action.EXECUTE);

        runUntilDone();

        assertThat(inventory.output(0).contents()).contains(new ResourceAmount(DIRT, 3));
        assertThat(inventory.input(0).isEmpty() && inventory.input(1).isEmpty()).isTrue();
    }

    @Test
    void severalInputsPutInGoToSlotsOfTheirOwnWhateverTheMode() {
        inventory.setMode(InputMode.SPLIT);

        inventory.insert(STONE, 4, Action.EXECUTE, NOBODY);
        inventory.insert(SAND, 2, Action.EXECUTE, NOBODY);

        assertThat(inventory.input(0).amount()).isEqualTo(4);
        assertThat(inventory.input(1).amount()).isEqualTo(2);
        assertThat(inventory.input(2).isEmpty()).isTrue();
    }

    @Test
    void aShapeHoldsTheLinesOfATierToItsMost() {
        final MachineShape shape = new MachineShape(3, 1);

        assertThat(shape.linesOf(MachineTier.QUANTUM)).isEqualTo(1);
        assertThat(MachineShape.SINGLE_INPUT.linesOf(MachineTier.QUANTUM)).isEqualTo(MachineTier.QUANTUM.linePairs());
    }
}
