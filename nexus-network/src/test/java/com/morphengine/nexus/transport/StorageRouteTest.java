package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.storage.CellStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static com.morphengine.nexus.test.TestResources.WATER;
import static org.assertj.core.api.Assertions.assertThat;

class StorageRouteTest {

    @Test
    void heldIsWhatTheSourceHolds() {
        final StorageRoute route = new StorageRoute(holding(STONE, 20), empty(), Actor.NOBODY);

        assertThat(route.held(STONE)).isEqualTo(20);
        assertThat(route.held(DIRT)).isZero();
    }

    @Test
    void movesTheRequestedAmount() {
        final CellStorage source = holding(STONE, 20);
        final CellStorage destination = empty();

        final long moved = new StorageRoute(source, destination, Actor.NOBODY).move(STONE, 5);

        assertThat(moved).isEqualTo(5);
        assertThat(source.amountOf(STONE)).isEqualTo(15);
        assertThat(destination.amountOf(STONE)).isEqualTo(5);
    }

    @Test
    void movesNoMoreThanTheSourceHolds() {
        final CellStorage destination = empty();

        final long moved = new StorageRoute(holding(STONE, 3), destination, Actor.NOBODY).move(STONE, 5);

        assertThat(moved).isEqualTo(3);
        assertThat(destination.amountOf(STONE)).isEqualTo(3);
    }

    @Test
    void movesOnlyWhatFitsTheDestination() {
        final CellStorage source = holding(STONE, 500);
        final CellStorage destination = holding(STONE, 440);

        final long moved = new StorageRoute(source, destination, Actor.NOBODY).move(STONE, 64);

        assertThat(moved).isEqualTo(8);
        assertThat(source.amountOf(STONE)).isEqualTo(492);
    }

    @Test
    void movesNothingTheDestinationRefuses() {
        final CellStorage source = holding(WATER, 5);

        final long moved = new StorageRoute(source, empty(), Actor.NOBODY).move(WATER, 5);

        assertThat(moved).isZero();
        assertThat(source.amountOf(WATER)).isEqualTo(5);
    }

    @Test
    void movesNothingOfAMissingResource() {
        final CellStorage destination = empty();

        final long moved = new StorageRoute(holding(STONE, 5), destination, Actor.NOBODY).move(DIRT, 5);

        assertThat(moved).isZero();
        assertThat(destination.contents()).isEmpty();
    }

    @Test
    void returnsWhatTheDestinationRefusesAfterPromisingRoom() {
        final CellStorage source = holding(STONE, 10);
        final Storage takesHalf = new TakesHalfOnExecute(empty());

        final long moved = new StorageRoute(source, takesHalf, Actor.NOBODY).move(STONE, 10);

        assertThat(moved).isEqualTo(5);
        assertThat(source.amountOf(STONE)).isEqualTo(5);
        assertThat(takesHalf.amountOf(STONE)).isEqualTo(5);
    }

    private static CellStorage empty() {
        return new CellStorage(ITEMS, SMALL, List.of());
    }

    private static CellStorage holding(final ResourceKey resource, final long amount) {
        return new CellStorage(resource.type(), SMALL, List.of(new ResourceAmount(resource, amount)));
    }

    /**
     * A storage whose simulated insert promises room it does not fully give.
     */
    private record TakesHalfOnExecute(Storage delegate) implements Storage {

        @Override
        public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
            final long offered = action.isExecute() ? Math.max(1, amount / 2) : amount;
            return delegate.insert(resource, offered, action, actor);
        }

        @Override
        public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
            return delegate.extract(resource, amount, action, actor);
        }

        @Override
        public long amountOf(final ResourceKey resource) {
            return delegate.amountOf(resource);
        }

        @Override
        public List<ResourceAmount> contents() {
            return delegate.contents();
        }
    }
}
