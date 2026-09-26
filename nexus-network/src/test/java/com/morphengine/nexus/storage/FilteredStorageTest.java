package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Actor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.morphengine.nexus.storage.TestResources.DIRT;
import static com.morphengine.nexus.storage.TestResources.ITEMS;
import static com.morphengine.nexus.storage.TestResources.SMALL;
import static com.morphengine.nexus.storage.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class FilteredStorageTest {

    @Test
    void whitelistTakesOnlyListedResources() {
        final FilteredStorage storage = filtered(FilterMode.ALLOW);

        assertThat(storage.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY)).isEqualTo(5);
        assertThat(storage.insert(DIRT, 5, Action.EXECUTE, Actor.NOBODY)).isZero();
    }

    @Test
    void blacklistRefusesListedResources() {
        final FilteredStorage storage = filtered(FilterMode.DENY);

        assertThat(storage.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(storage.insert(DIRT, 5, Action.EXECUTE, Actor.NOBODY)).isEqualTo(5);
    }

    @Test
    void extractIgnoresTheFilter() {
        final CellStorage cell = new CellStorage(ITEMS, SMALL, List.of());
        cell.insert(DIRT, 5, Action.EXECUTE, Actor.NOBODY);
        final FilteredStorage storage =
                new FilteredStorage(cell, new ResourceFilter(FilterMode.ALLOW, Set.of(STONE)));

        assertThat(storage.extract(DIRT, 5, Action.EXECUTE, Actor.NOBODY)).isEqualTo(5);
    }

    @Test
    void whitelistReservesTheStorageForListedResources() {
        assertThat(filtered(FilterMode.ALLOW).isReservedFor(STONE)).isTrue();
        assertThat(filtered(FilterMode.ALLOW).isReservedFor(DIRT)).isFalse();
        assertThat(filtered(FilterMode.DENY).isReservedFor(DIRT)).isFalse();
    }

    private static FilteredStorage filtered(final FilterMode mode) {
        return new FilteredStorage(new CellStorage(ITEMS, SMALL, List.of()),
                new ResourceFilter(mode, Set.of(STONE)));
    }
}
