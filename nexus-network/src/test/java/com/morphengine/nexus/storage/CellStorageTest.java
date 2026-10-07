package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.CellStatus;
import com.morphengine.nexus.api.storage.CellUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static com.morphengine.nexus.test.TestResources.WATER;
import static com.morphengine.nexus.test.TestResources.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellStorageTest {

    @Test
    void emptyCellHasRoom() {
        final CellUsage usage = emptyCell().usage();

        assertThat(usage).isEqualTo(new CellUsage(0, 64, 0, 4, CellStatus.HAS_ROOM));
    }

    @Test
    void firstTypeReservesItsBytes() {
        final CellStorage cell = emptyCell();

        cell.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(cell.amountOf(STONE)).isEqualTo(10);
        assertThat(cell.usage().usedBytes()).isEqualTo(8 + 2);
        assertThat(cell.usage().storedTypes()).isEqualTo(1);
    }

    @Test
    void singleTypeFillsAllBytesLeftAfterItsReservation() {
        final CellStorage cell = emptyCell();

        final long accepted = cell.insert(STONE, 1000, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo((64 - 8) * 8);
        assertThat(cell.usage().status()).isEqualTo(CellStatus.BYTES_FULL);
    }

    @Test
    void everyNewTypeLeavesLessRoomForUnits() {
        final CellStorage cell = emptyCell();
        cell.insert(STONE, 1, Action.EXECUTE, Actor.NOBODY);
        cell.insert(DIRT, 1, Action.EXECUTE, Actor.NOBODY);
        cell.insert(SAND, 1, Action.EXECUTE, Actor.NOBODY);

        final long accepted = cell.insert(STONE, 1000, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo((64 - 3 * 8) * 8 - 3);
    }

    @Test
    void noNewTypeFitsOnceAllTypesAreTaken() {
        final CellStorage cell = emptyCell();
        for (String name : List.of("a", "b", "c", "d")) {
            cell.insert(item(name), 1, Action.EXECUTE, Actor.NOBODY);
        }

        assertThat(cell.insert(STONE, 1, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(cell.insert(item("a"), 1, Action.EXECUTE, Actor.NOBODY)).isEqualTo(1);
        assertThat(cell.usage().status()).isEqualTo(CellStatus.TYPES_FULL);
    }

    @Test
    void cellWithNoTypeAndNoByteLeftIsFull() {
        final CellStorage cell = emptyCell();
        for (String name : List.of("a", "b", "c", "d")) {
            cell.insert(item(name), 1, Action.EXECUTE, Actor.NOBODY);
        }
        cell.insert(item("a"), 1000, Action.EXECUTE, Actor.NOBODY);

        assertThat(cell.usage()).isEqualTo(new CellUsage(64, 64, 4, 4, CellStatus.FULL));
    }

    @Test
    void resourceOfAnotherTypeIsRefused() {
        final CellStorage cell = emptyCell();

        assertThat(cell.insert(WATER, 5, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(cell.contents()).isEmpty();
    }

    @Test
    void simulatedInsertChangesNothing() {
        final CellStorage cell = emptyCell();

        final long accepted = cell.insert(STONE, 20, Action.SIMULATE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(20);
        assertThat(cell.amountOf(STONE)).isZero();
    }

    @Test
    void extractTakesAtMostWhatIsStored() {
        final CellStorage cell = emptyCell();
        cell.insert(STONE, 7, Action.EXECUTE, Actor.NOBODY);

        assertThat(cell.extract(STONE, 5, Action.EXECUTE, Actor.NOBODY)).isEqualTo(5);
        assertThat(cell.extract(STONE, 5, Action.EXECUTE, Actor.NOBODY)).isEqualTo(2);
        assertThat(cell.extract(DIRT, 5, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(cell.usage().storedTypes()).isZero();
    }

    @Test
    void extractingLastUnitsFreesTheType() {
        final CellStorage cell = emptyCell();
        for (String name : List.of("a", "b", "c", "d")) {
            cell.insert(item(name), 1, Action.EXECUTE, Actor.NOBODY);
        }

        cell.extract(item("a"), 1, Action.EXECUTE, Actor.NOBODY);

        assertThat(cell.insert(STONE, 1, Action.EXECUTE, Actor.NOBODY)).isEqualTo(1);
    }

    @Test
    void contentsOverCapacityAreKeptAndShownFull() {
        final List<ResourceAmount> saved = List.of("a", "b", "c", "d", "e").stream()
                .map(name -> new ResourceAmount(item(name), 100)).toList();

        final CellStorage cell = new CellStorage(ITEMS, SMALL, saved);

        assertThat(cell.contents()).hasSize(5);
        assertThat(cell.usage().status()).isEqualTo(CellStatus.FULL);
        assertThat(cell.insert(item("a"), 1, Action.EXECUTE, Actor.NOBODY)).isZero();
    }

    @Test
    void nonPositiveAmountIsRejected() {
        final CellStorage cell = emptyCell();

        assertThatThrownBy(() -> cell.insert(STONE, 0, Action.EXECUTE, Actor.NOBODY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static CellStorage emptyCell() {
        return new CellStorage(ITEMS, SMALL, List.of());
    }
}
