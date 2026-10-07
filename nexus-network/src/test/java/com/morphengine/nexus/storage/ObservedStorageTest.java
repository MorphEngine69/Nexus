package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static com.morphengine.nexus.test.TestResources.WATER;
import static org.assertj.core.api.Assertions.assertThat;

class ObservedStorageTest {

    private final AtomicInteger changes = new AtomicInteger();
    private final ObservedStorage storage =
            new ObservedStorage(new CellStorage(ITEMS, SMALL, List.of()), changes::incrementAndGet);

    @Test
    void executedChangesAreReported() {
        storage.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY);
        storage.extract(STONE, 2, Action.EXECUTE, Actor.NOBODY);

        assertThat(changes).hasValue(2);
    }

    @Test
    void simulatedOperationsAreNotReported() {
        storage.insert(STONE, 5, Action.SIMULATE, Actor.NOBODY);

        assertThat(changes).hasValue(0);
    }

    @Test
    void operationsThatMoveNothingAreNotReported() {
        storage.insert(WATER, 5, Action.EXECUTE, Actor.NOBODY);
        storage.extract(STONE, 5, Action.EXECUTE, Actor.NOBODY);

        assertThat(changes).hasValue(0);
    }
}
