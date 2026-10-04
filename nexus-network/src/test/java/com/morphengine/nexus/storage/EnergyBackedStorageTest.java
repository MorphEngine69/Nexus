package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.energy.EnergyPool;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.energy.StorageEnergyBuffer;
import com.morphengine.nexus.security.AccessGate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.morphengine.nexus.test.TestActors.actingFor;
import static com.morphengine.nexus.test.TestResources.ENERGY;
import static com.morphengine.nexus.test.TestResources.ENERGY_CELL;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class EnergyBackedStorageTest {

    private static final UUID PLAYER = new UUID(0, 1);

    private final NetworkStorage storage = new NetworkStorage();
    private final SimpleEnergyBuffer cell = new SimpleEnergyBuffer(1000, 1000, 1000);
    private final EnergyPool pool = new EnergyPool(List.of(cell, new StorageEnergyBuffer(storage, ENERGY,
            Actor.NOBODY)));
    private final EnergyBackedStorage resources = new EnergyBackedStorage(storage, pool, ENERGY,
            AccessGate.UNRESTRICTED);

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

    @Test
    void gateTurnsAwayEnergyForAPlayerWithoutTheRight() {
        pool.insert(500, Action.EXECUTE);
        final EnergyBackedStorage guarded = new EnergyBackedStorage(storage, pool, ENERGY,
                AccessGate.of((player, permission) -> false));

        assertThat(guarded.extract(ENERGY, 100, Action.EXECUTE, actingFor(PLAYER))).isZero();
        assertThat(guarded.insert(ENERGY, 100, Action.EXECUTE, actingFor(PLAYER))).isZero();
        assertThat(pool.stored()).isEqualTo(500);
    }

    @Test
    void gateLeavesOtherResourcesToTheStorage() {
        final EnergyBackedStorage guarded = new EnergyBackedStorage(storage, pool, ENERGY,
                AccessGate.of((player, permission) -> false));

        assertThat(guarded.insert(STONE, 5, Action.EXECUTE, actingFor(PLAYER))).isEqualTo(5);
    }
}
