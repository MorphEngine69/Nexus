package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import org.junit.jupiter.api.Test;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineSlotTest {

    private static final long LIMIT = 64;

    private final MachineSlot slot = new PlainMachineSlot(resource -> LIMIT);

    @Test
    void anEmptySlotTakesUpToItsLimit() {
        final long accepted = slot.insert(STONE, 100, Action.EXECUTE);

        assertThat(accepted).isEqualTo(LIMIT);
        assertThat(slot.contents()).contains(new ResourceAmount(STONE, LIMIT));
    }

    @Test
    void aSlotWithAnotherResourceTakesNothing() {
        slot.insert(STONE, 10, Action.EXECUTE);

        assertThat(slot.insert(DIRT, 10, Action.EXECUTE)).isZero();
        assertThat(slot.room(DIRT)).isZero();
    }

    @Test
    void aSimulatedInsertChangesNothing() {
        final long accepted = slot.insert(STONE, 10, Action.SIMULATE);

        assertThat(accepted).isEqualTo(10);
        assertThat(slot.isEmpty()).isTrue();
    }

    @Test
    void roomIsTheLimitLessWhatIsThere() {
        slot.insert(STONE, 40, Action.EXECUTE);

        assertThat(slot.room(STONE)).isEqualTo(LIMIT - 40);
    }

    @Test
    void extractingEverythingEmptiesTheSlot() {
        slot.insert(STONE, 10, Action.EXECUTE);

        final long taken = slot.extract(STONE, 50, Action.EXECUTE);

        assertThat(taken).isEqualTo(10);
        assertThat(slot.isEmpty()).isTrue();
        assertThat(slot.resource()).isNull();
    }

    @Test
    void extractingAnotherResourceGivesNothing() {
        slot.insert(STONE, 10, Action.EXECUTE);

        assertThat(slot.extract(DIRT, 5, Action.EXECUTE)).isZero();
        assertThat(slot.amount()).isEqualTo(10);
    }

    @Test
    void aSimulatedExtractChangesNothing() {
        slot.insert(STONE, 10, Action.EXECUTE);

        assertThat(slot.extract(STONE, 4, Action.SIMULATE)).isEqualTo(4);
        assertThat(slot.amount()).isEqualTo(10);
    }

    @Test
    void theLimitMayDependOnTheResource() {
        final MachineSlot picky = new PlainMachineSlot(resource -> resource.equals(STONE) ? 16 : 1);

        assertThat(picky.room(STONE)).isEqualTo(16);
        assertThat(picky.room(DIRT)).isEqualTo(1);
    }

    @Test
    void restoreBringsBackSavedContentsAndZeroEmptiesTheSlot() {
        slot.restore(STONE, 7);
        assertThat(slot.contents()).contains(new ResourceAmount(STONE, 7));

        slot.restore(STONE, 0);

        assertThat(slot.isEmpty()).isTrue();
    }

    @Test
    void nonPositiveAmountsAreRefused() {
        assertThatThrownBy(() -> slot.insert(STONE, 0, Action.EXECUTE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> slot.extract(STONE, -1, Action.EXECUTE)).isInstanceOf(IllegalArgumentException.class);
    }
}
