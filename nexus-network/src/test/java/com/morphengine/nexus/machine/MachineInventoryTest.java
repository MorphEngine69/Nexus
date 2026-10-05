package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.test.TestResources;
import org.junit.jupiter.api.Test;

import static com.morphengine.nexus.api.storage.Actor.NOBODY;
import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineInventoryTest {

    private static final long LIMIT = 64;

    private final MachineInventory inventory = new MachineInventory(3, new PlainMachineSlots(resource -> LIMIT));

    private long put(final ResourceKey resource, final long amount) {
        return inventory.insert(resource, amount, Action.EXECUTE, NOBODY);
    }

    @Test
    void perResourceEachNewResourceTakesASlotOfItsOwn() {
        put(STONE, 10);
        put(DIRT, 20);

        assertThat(inventory.input(0).contents()).contains(new ResourceAmount(STONE, 10));
        assertThat(inventory.input(1).contents()).contains(new ResourceAmount(DIRT, 20));
        assertThat(inventory.input(2).isEmpty()).isTrue();
    }

    @Test
    void perResourceAResourceAlreadyInASlotGoesToThatSlot() {
        put(STONE, 10);
        put(DIRT, 20);

        put(STONE, 5);

        assertThat(inventory.input(0).amount()).isEqualTo(15);
        assertThat(inventory.input(2).isEmpty()).isTrue();
    }

    @Test
    void perResourceAFullSlotDoesNotSpillIntoAFreeOne() {
        put(STONE, LIMIT);

        final long accepted = put(STONE, 10);

        assertThat(accepted).isZero();
        assertThat(inventory.input(1).isEmpty()).isTrue();
    }

    @Test
    void perResourceRefusesAResourceWhenEverySlotHoldsAnother() {
        put(STONE, 1);
        put(DIRT, 1);
        put(SAND, 1);

        assertThat(put(TestResources.item("gravel"), 1)).isZero();
    }

    @Test
    void splitSpreadsTheFirstResourceEvenlyOverAllSlots() {
        inventory.setMode(InputMode.SPLIT);

        final long accepted = put(STONE, 10);

        assertThat(accepted).isEqualTo(10);
        assertThat(inventory.input(0).amount()).isEqualTo(4);
        assertThat(inventory.input(1).amount()).isEqualTo(3);
        assertThat(inventory.input(2).amount()).isEqualTo(3);
    }

    @Test
    void splitSpreadsUnitsThatArriveOneAtATimeAsAWholeStackDoes() {
        inventory.setMode(InputMode.SPLIT);

        for (int unit = 0; unit < 10; unit++) {
            put(STONE, 1);
        }

        assertThat(inventory.input(0).amount()).isEqualTo(4);
        assertThat(inventory.input(1).amount()).isEqualTo(3);
        assertThat(inventory.input(2).amount()).isEqualTo(3);
    }

    @Test
    void splitFillsTheEmptierSlotsFirstWhenTheSlotsHoldUnequalAmounts() {
        inventory.setMode(InputMode.SPLIT);
        inventory.input(0).restore(STONE, 10);

        put(STONE, 12);

        assertThat(inventory.input(0).amount()).isEqualTo(10);
        assertThat(inventory.input(1).amount()).isEqualTo(6);
        assertThat(inventory.input(2).amount()).isEqualTo(6);
    }

    @Test
    void splitRefusesAnotherResourceWhileTheSlotsHoldOne() {
        inventory.setMode(InputMode.SPLIT);
        put(STONE, 3);

        assertThat(put(DIRT, 3)).isZero();
        assertThat(put(STONE, 3)).isEqualTo(3);
    }

    @Test
    void splitHandsWhatAFullSlotCannotTakeToTheOthers() {
        inventory.setMode(InputMode.SPLIT);
        inventory.input(0).restore(STONE, LIMIT);

        final long accepted = put(STONE, 9);

        assertThat(accepted).isEqualTo(9);
        assertThat(inventory.input(0).amount()).isEqualTo(LIMIT);
        assertThat(inventory.input(1).amount() + inventory.input(2).amount()).isEqualTo(9);
    }

    @Test
    void splitTakesOnlyWhatFitsInAllTheSlots() {
        inventory.setMode(InputMode.SPLIT);

        assertThat(put(STONE, LIMIT * 3 + 50)).isEqualTo(LIMIT * 3);
    }

    @Test
    void aSimulatedInsertChangesNothing() {
        inventory.setMode(InputMode.SPLIT);

        final long accepted = inventory.insert(STONE, 10, Action.SIMULATE, NOBODY);

        assertThat(accepted).isEqualTo(10);
        assertThat(inventory.input(0).isEmpty()).isTrue();
    }

    @Test
    void emptiedSlotsTakeAnotherResourceAgainInSplitMode() {
        inventory.setMode(InputMode.SPLIT);
        put(STONE, 3);
        for (int index = 0; index < 3; index++) {
            inventory.input(index).extract(STONE, 1, Action.EXECUTE);
        }

        assertThat(put(DIRT, 6)).isEqualTo(6);
    }

    @Test
    void extractTakesFromTheOutputSlotsAndNeverFromTheInputs() {
        put(STONE, 10);
        inventory.output(0).restore(DIRT, 5);
        inventory.output(2).restore(DIRT, 7);

        assertThat(inventory.extract(DIRT, 100, Action.EXECUTE, NOBODY)).isEqualTo(12);
        assertThat(inventory.extract(STONE, 100, Action.EXECUTE, NOBODY)).isZero();
        assertThat(inventory.input(0).amount()).isEqualTo(10);
    }

    @Test
    void extractStopsAtWhatWasAsked() {
        inventory.output(0).restore(DIRT, 5);
        inventory.output(1).restore(DIRT, 7);

        assertThat(inventory.extract(DIRT, 8, Action.EXECUTE, NOBODY)).isEqualTo(8);
        assertThat(inventory.output(1).amount()).isEqualTo(4);
    }

    @Test
    void growingKeepsWhatTheSlotsHold() {
        put(STONE, 10);

        inventory.grow(5);

        assertThat(inventory.lineCount()).isEqualTo(5);
        assertThat(inventory.input(0).amount()).isEqualTo(10);
    }

    @Test
    void shrinkingIsRefused() {
        assertThatThrownBy(() -> inventory.grow(2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planInsertShowsTheSpreadWithoutChangingAnything() {
        inventory.setMode(InputMode.SPLIT);

        final long[] parts = inventory.planInsert(STONE, 10);

        assertThat(parts).containsExactly(4, 3, 3);
        assertThat(inventory.input(0).isEmpty()).isTrue();
    }

    @Test
    void planInsertPerResourceNamesTheOneSlotThatTakesIt() {
        put(STONE, 10);

        assertThat(inventory.planInsert(STONE, 5)).containsExactly(5, 0, 0);
        assertThat(inventory.planInsert(DIRT, 5)).containsExactly(0, 5, 0);
    }
}
