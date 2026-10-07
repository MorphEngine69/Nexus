package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.storage.CellStorage;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.storage.SingleResourceCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.ENERGY;
import static com.morphengine.nexus.test.TestResources.ENERGY_CELL;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageEnergyBufferTest {

    @Test
    void storedIsTheEnergyTheStorageHolds() {
        final NetworkStorage storage = storageWithEnergyCell();
        storage.insert(ENERGY, 120, Action.EXECUTE, Actor.NOBODY);

        assertThat(bufferOver(storage).stored()).isEqualTo(120);
    }

    @Test
    void capacityIsStoredPlusRoomLeft() {
        final NetworkStorage storage = storageWithEnergyCell();
        storage.insert(ENERGY, 120, Action.EXECUTE, Actor.NOBODY);

        assertThat(bufferOver(storage).capacity()).isEqualTo(400);
    }

    @Test
    void storageWithoutEnergyCellsHasNoCapacity() {
        final NetworkStorage storage = new NetworkStorage();
        storage.addSource(new CellStorage(ITEMS, SMALL, List.of()), 0);

        assertThat(bufferOver(storage).capacity()).isZero();
    }

    @Test
    void insertGoesIntoTheStorageAndIsCounted() {
        final NetworkStorage storage = storageWithEnergyCell();
        final StorageEnergyBuffer buffer = bufferOver(storage);

        final long accepted = buffer.insert(500, Action.EXECUTE);

        assertThat(accepted).isEqualTo(400);
        assertThat(storage.amountOf(ENERGY)).isEqualTo(400);
        assertThat(buffer.totalInserted()).isEqualTo(400);
    }

    @Test
    void extractTakesFromTheStorageAndIsCounted() {
        final NetworkStorage storage = storageWithEnergyCell();
        storage.insert(ENERGY, 300, Action.EXECUTE, Actor.NOBODY);
        final StorageEnergyBuffer buffer = bufferOver(storage);

        final long removed = buffer.extract(100, Action.EXECUTE);

        assertThat(removed).isEqualTo(100);
        assertThat(storage.amountOf(ENERGY)).isEqualTo(200);
        assertThat(buffer.totalExtracted()).isEqualTo(100);
    }

    @Test
    void simulationChangesNothingAndCountsNothing() {
        final NetworkStorage storage = storageWithEnergyCell();
        final StorageEnergyBuffer buffer = bufferOver(storage);

        buffer.insert(100, Action.SIMULATE);

        assertThat(storage.amountOf(ENERGY)).isZero();
        assertThat(buffer.totalInserted()).isZero();
    }

    @Test
    void zeroAmountMovesNothing() {
        final StorageEnergyBuffer buffer = bufferOver(storageWithEnergyCell());

        assertThat(buffer.insert(0, Action.EXECUTE)).isZero();
        assertThat(buffer.extract(0, Action.EXECUTE)).isZero();
    }

    @Test
    void negativeAmountIsRejected() {
        final StorageEnergyBuffer buffer = bufferOver(storageWithEnergyCell());

        assertThatThrownBy(() -> buffer.insert(-1, Action.EXECUTE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void otherResourcesOfTheStorageAreNotEnergy() {
        final NetworkStorage storage = storageWithEnergyCell();
        storage.addSource(new CellStorage(ITEMS, SMALL, List.of()), 0);
        storage.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(bufferOver(storage).stored()).isZero();
    }

    private static NetworkStorage storageWithEnergyCell() {
        final NetworkStorage storage = new NetworkStorage();
        storage.addSource(new SingleResourceCell(ENERGY, ENERGY_CELL, List.of()), 0);
        return storage;
    }

    private static StorageEnergyBuffer bufferOver(final NetworkStorage storage) {
        return new StorageEnergyBuffer(storage, ENERGY, Actor.NOBODY);
    }
}
