package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.FLUIDS;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static com.morphengine.nexus.test.TestResources.WATER;
import static org.assertj.core.api.Assertions.assertThat;

class SingleTypeStorageTest {

    private final NetworkStorage network = new NetworkStorage();
    private final SingleTypeStorage fluids = new SingleTypeStorage(network, FLUIDS);

    SingleTypeStorageTest() {
        network.addSource(new CellStorage(ITEMS, SMALL, List.of()), 0);
        network.addSource(new CellStorage(FLUIDS, SMALL, List.of()), 0);
        network.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY);
        network.insert(WATER, 7, Action.EXECUTE, Actor.NOBODY);
    }

    @Test
    void contentsListOnlyItsKind() {
        assertThat(fluids.contents()).containsExactly(new ResourceAmount(WATER, 7));
    }

    @Test
    void otherKindsLookAbsent() {
        assertThat(fluids.amountOf(STONE)).isZero();
        assertThat(fluids.amountOf(WATER)).isEqualTo(7);
    }

    @Test
    void otherKindsAreNeitherAcceptedNorGiven() {
        assertThat(fluids.insert(STONE, 1, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(fluids.extract(STONE, 1, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(network.amountOf(STONE)).isEqualTo(5);
    }

    @Test
    void itsKindPassesThrough() {
        assertThat(fluids.extract(WATER, 3, Action.EXECUTE, Actor.NOBODY)).isEqualTo(3);
        assertThat(fluids.insert(WATER, 2, Action.EXECUTE, Actor.NOBODY)).isEqualTo(2);
        assertThat(network.amountOf(WATER)).isEqualTo(6);
    }
}
