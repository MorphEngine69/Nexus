package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import org.junit.jupiter.api.Test;

import static com.morphengine.nexus.machine.MachineTestRecipes.STONE_ENERGY;
import static com.morphengine.nexus.machine.MachineTestRecipes.STONE_TICKS;
import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineLineTest {

    private static final long LIMIT = 64;
    private static final int NORMAL = 100;
    private static final long PLENTY = 100_000;
    private static final int GIVE_UP = 10_000;

    private final MachineSlot input = new PlainMachineSlot(resource -> LIMIT);
    private final MachineSlot output = new PlainMachineSlot(resource -> LIMIT);
    private final MachineLine line = new MachineLine(input, output, MachineTestRecipes.ALL);
    private final SimpleEnergyBuffer energy = new SimpleEnergyBuffer(PLENTY, PLENTY, PLENTY);

    private void charge(final long amount) {
        energy.insert(amount, Action.EXECUTE);
    }

    private int ticksUntilDone(final int speedPercent) {
        int ticks = 0;
        while (output.isEmpty() && ticks < GIVE_UP) {
            line.tick(energy, speedPercent);
            ticks++;
        }
        return ticks;
    }

    @Test
    void aLineWithNothingInItsInputDoesNothing() {
        charge(PLENTY);

        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.IDLE);
        assertThat(energy.stored()).isEqualTo(PLENTY);
    }

    @Test
    void aRecipeTakesItsTicksAndGivesItsResult() {
        charge(PLENTY);
        input.insert(STONE, 3, Action.EXECUTE);

        final int ticks = ticksUntilDone(NORMAL);

        assertThat(ticks).isEqualTo(STONE_TICKS);
        assertThat(output.contents()).contains(new ResourceAmount(DIRT, 2));
        assertThat(input.amount()).isEqualTo(2);
    }

    @Test
    void theRecipeCostsItsFullEnergyAtNormalSpeed() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);

        ticksUntilDone(NORMAL);

        assertThat(PLENTY - energy.stored()).isEqualTo(STONE_TICKS * STONE_ENERGY);
    }

    @Test
    void aFasterLineFinishesSoonerAndPaysTheSameInAll() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);

        final int ticks = ticksUntilDone(2 * NORMAL);

        assertThat(ticks).isEqualTo(STONE_TICKS / 2);
        assertThat(PLENTY - energy.stored()).isEqualTo(STONE_TICKS * STONE_ENERGY);
    }

    @Test
    void aTickWithoutEnoughEnergyDoesNoWork() {
        input.insert(STONE, 1, Action.EXECUTE);
        charge(STONE_ENERGY - 1);

        final MachineActivity activity = line.tick(energy, NORMAL);

        assertThat(activity).isEqualTo(MachineActivity.WAITING_FOR_ENERGY);
        assertThat(line.progress()).isZero();
        assertThat(energy.stored()).isEqualTo(STONE_ENERGY - 1);
    }

    @Test
    void theLineGoesOnWhenEnergyComesBack() {
        input.insert(STONE, 1, Action.EXECUTE);
        line.tick(energy, NORMAL);
        charge(PLENTY);

        final int ticks = ticksUntilDone(NORMAL);

        assertThat(ticks).isEqualTo(STONE_TICKS);
    }

    @Test
    void aLineDoesNotStartWhenTheOutputHasNoRoomForTheResult() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);
        output.insert(SAND, LIMIT, Action.EXECUTE);

        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.OUTPUT_BLOCKED);
        assertThat(line.isRunning()).isFalse();
        assertThat(energy.stored()).isEqualTo(PLENTY);
    }

    @Test
    void aFinishedJobWaitsForRoomAndThenCompletes() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);
        output.insert(DIRT, LIMIT - 2, Action.EXECUTE);
        for (int tick = 0; tick < STONE_TICKS - 1; tick++) {
            line.tick(energy, NORMAL);
        }
        output.insert(DIRT, 2, Action.EXECUTE);

        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.OUTPUT_BLOCKED);
        final long spent = PLENTY - energy.stored();
        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.OUTPUT_BLOCKED);
        assertThat(PLENTY - energy.stored()).isEqualTo(spent);
        output.extract(DIRT, 10, Action.EXECUTE);

        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.WORKING);
        assertThat(input.isEmpty()).isTrue();
        assertThat(output.amount()).isEqualTo(LIMIT - 10 + 2);
    }

    @Test
    void takingTheInputAwayStopsTheJob() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);
        line.tick(energy, NORMAL);
        input.extract(STONE, 1, Action.EXECUTE);

        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.IDLE);
        assertThat(line.isRunning()).isFalse();
        assertThat(line.progress()).isZero();
    }

    @Test
    void aRecipeThatNeedsMoreThanThereIsDoesNotStart() {
        charge(PLENTY);
        input.insert(DIRT, 1, Action.EXECUTE);

        assertThat(line.tick(energy, NORMAL)).isEqualTo(MachineActivity.IDLE);
    }

    @Test
    void progressIsReportedInPercent() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);
        for (int tick = 0; tick < STONE_TICKS / 2; tick++) {
            line.tick(energy, NORMAL);
        }

        assertThat(line.progressPercent()).isEqualTo(50);
    }

    @Test
    void savedProgressIsTakenUpAgainWhenTheJobStarts() {
        charge(PLENTY);
        input.insert(STONE, 1, Action.EXECUTE);
        line.restoreProgress(NORMAL * (STONE_TICKS - 2L));

        final int ticks = ticksUntilDone(NORMAL);

        assertThat(ticks).isEqualTo(2);
    }

    @Test
    void aSpeedOfZeroIsRefused() {
        assertThatThrownBy(() -> line.tick(energy, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
