package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.api.storage.CellStatus;
import com.morphengine.nexus.api.storage.CellUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.ENERGY;
import static com.morphengine.nexus.test.TestResources.ENERGY_CELL;
import static com.morphengine.nexus.test.TestResources.ENERGY_TYPE;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleResourceCellTest {

    @Test
    void emptyCellHasRoom() {
        final CellUsage usage = emptyCell().usage();

        assertThat(usage).isEqualTo(new CellUsage(0, 4, 0, 1, CellStatus.HAS_ROOM));
    }

    @Test
    void capacityIsEveryByteWithoutTypeReservation() {
        assertThat(emptyCell().capacity()).isEqualTo(400);
    }

    @Test
    void typeIsTheTypeOfItsResource() {
        assertThat(emptyCell().type()).isSameAs(ENERGY_TYPE);
    }

    @Test
    void insertFillsUpToCapacity() {
        final SingleResourceCell cell = emptyCell();

        final long accepted = cell.insert(ENERGY, 1000, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(400);
        assertThat(cell.amountOf(ENERGY)).isEqualTo(400);
    }

    @Test
    void partlyFilledCellStaysGreenInsteadOfTypesFull() {
        final SingleResourceCell cell = emptyCell();

        cell.insert(ENERGY, 150, Action.EXECUTE, Actor.NOBODY);

        assertThat(cell.usage()).isEqualTo(new CellUsage(2, 4, 1, 1, CellStatus.HAS_ROOM));
    }

    @Test
    void fullCellIsFull() {
        final SingleResourceCell cell = emptyCell();

        cell.insert(ENERGY, 400, Action.EXECUTE, Actor.NOBODY);

        assertThat(cell.usage().status()).isEqualTo(CellStatus.FULL);
        assertThat(cell.insert(ENERGY, 1, Action.SIMULATE, Actor.NOBODY)).isZero();
    }

    @Test
    void otherResourcesAreRefused() {
        final SingleResourceCell cell = emptyCell();

        final long accepted = cell.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isZero();
        assertThat(cell.contents()).isEmpty();
    }

    @Test
    void simulatedInsertChangesNothing() {
        final SingleResourceCell cell = emptyCell();

        final long accepted = cell.insert(ENERGY, 100, Action.SIMULATE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(100);
        assertThat(cell.amountOf(ENERGY)).isZero();
    }

    @Test
    void extractTakesAtMostWhatIsStored() {
        final SingleResourceCell cell = cellHolding(250);

        final long removed = cell.extract(ENERGY, 1000, Action.EXECUTE, Actor.NOBODY);

        assertThat(removed).isEqualTo(250);
        assertThat(cell.contents()).isEmpty();
    }

    @Test
    void contentsOverCapacityAreKeptAndShownFull() {
        final SingleResourceCell cell = cellHolding(1000);

        assertThat(cell.amountOf(ENERGY)).isEqualTo(1000);
        assertThat(cell.insert(ENERGY, 1, Action.SIMULATE, Actor.NOBODY)).isZero();
        assertThat(cell.usage()).isEqualTo(new CellUsage(4, 4, 1, 1, CellStatus.FULL));
    }

    @Test
    void capacityPastLongStopsAtMaxValue() {
        final SingleResourceCell cell = new SingleResourceCell(ENERGY,
                new CellSpec(Long.MAX_VALUE / 2, 1, 1, 4), List.of());

        assertThat(cell.capacity()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void nonPositiveAmountIsRejected() {
        final SingleResourceCell cell = emptyCell();

        assertThatThrownBy(() -> cell.insert(ENERGY, 0, Action.EXECUTE, Actor.NOBODY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SingleResourceCell emptyCell() {
        return new SingleResourceCell(ENERGY, ENERGY_CELL, List.of());
    }

    private static SingleResourceCell cellHolding(final long amount) {
        return new SingleResourceCell(ENERGY, ENERGY_CELL, List.of(new ResourceAmount(ENERGY, amount)));
    }
}
