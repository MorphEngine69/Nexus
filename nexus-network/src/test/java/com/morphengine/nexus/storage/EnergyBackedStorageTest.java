package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.energy.EnergyPool;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.energy.StorageEnergyBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.ENERGY;
import static com.morphengine.nexus.test.TestResources.ENERGY_CELL;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class EnergyBackedStorageTest {

    private final NetworkStorage storage = new NetworkStorage();
    private final SimpleEnergyBuffer cell = new SimpleEnergyBuffer(1000, 1000, 1000);
    private final EnergyPool pool = new EnergyPool(List.of(cell, new StorageEnergyBuffer(storage, ENERGY,
            Actor.NOBODY)));
    private final EnergyBackedStorage resources = new EnergyBackedStorage(storage, pool, ENERGY);

    EnergyBackedStorageTest() {
        storage.addSource(new CellStorage(ITEMS, SMALL, List.of()), 0);
        storage.addSource(new SingleResourceCell(ENERGY, ENERGY_CELL, List.of()), 0);
    }

    @Test
    void energyInsertGoesIntoThePool() {
        final long accepted = resources.insert(ENERGY, 1200, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(1200);
        assertThat(cell.stored()).isEqualTo(1000);
        assertThat(storage.amountOf(ENERGY)).isEqualTo(200);
    }

    @Test
    void energyExtractTakesFromThePool() {
        pool.insert(1300, Action.EXECUTE);

        final long removed = resources.extract(ENERGY, 1100, Action.EXECUTE, Actor.NOBODY);

        assertThat(removed).isEqualTo(1100);
        assertThat(pool.stored()).isEqualTo(200);
    }

    @Test
    void energyAmountIsTheWholePool() {
        pool.insert(1300, Action.EXECUTE);

        assertThat(resources.amountOf(ENERGY)).isEqualTo(1300);
    }

    @Test
    void otherResourcesGoToTheStorage() {
        final long accepted = resources.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(10);
        assertThat(storage.amountOf(STONE)).isEqualTo(10);
        assertThat(resources.amountOf(STONE)).isEqualTo(10);
    }

    @Test
    void contentsListEnergyOnceWithThePoolTotal() {
        resources.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY);
        pool.insert(1300, Action.EXECUTE);

        assertThat(resources.contents()).containsExactly(
                new ResourceAmount(STONE, 5), new ResourceAmount(ENERGY, 1300));
    }

    @Test
    void contentsLeaveEnergyOutWhenThePoolIsEmpty() {
        resources.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY);

        assertThat(resources.contents()).containsExactly(new ResourceAmount(STONE, 5));
    }
}
