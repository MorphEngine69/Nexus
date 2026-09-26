package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.storage.CellStorage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static com.morphengine.nexus.test.TestResources.item;
import static org.assertj.core.api.Assertions.assertThat;

class SweepTaskTest {

    private static final TransferQuota FOUR = resource -> 4;

    @Test
    void pullsTheFirstResourceOfTheSourceUpToTheQuota() {
        final CellStorage source = chest(new ResourceAmount(STONE, 10), new ResourceAmount(DIRT, 10));
        final CellStorage network = empty();

        final long moved = new SweepTask(ResourceFilter.NONE, FOUR).runOnce(route(source, network));

        assertThat(moved).isEqualTo(4);
        assertThat(network.amountOf(STONE)).isEqualTo(4);
        assertThat(network.amountOf(DIRT)).isZero();
    }

    @Test
    void whitelistPullsOnlyListedResources() {
        final CellStorage source = chest(new ResourceAmount(STONE, 10), new ResourceAmount(DIRT, 10));
        final CellStorage network = empty();

        new SweepTask(new ResourceFilter(FilterMode.ALLOW, Set.of(DIRT)), FOUR).runOnce(route(source, network));

        assertThat(network.contents()).containsExactly(new ResourceAmount(DIRT, 4));
    }

    @Test
    void blacklistSkipsListedResources() {
        final CellStorage source = chest(new ResourceAmount(STONE, 10), new ResourceAmount(DIRT, 10));
        final CellStorage network = empty();

        new SweepTask(new ResourceFilter(FilterMode.DENY, Set.of(STONE)), FOUR).runOnce(route(source, network));

        assertThat(network.contents()).containsExactly(new ResourceAmount(DIRT, 4));
    }

    @Test
    void goesOnToTheNextResourceWhenTheNetworkRefusesOne() {
        final CellStorage source = chest(new ResourceAmount(STONE, 10), new ResourceAmount(DIRT, 10));
        final CellStorage network = new CellStorage(ITEMS, SMALL, List.of(
                new ResourceAmount(SAND, 1), new ResourceAmount(DIRT, 1), new ResourceAmount(item("clay"), 1),
                new ResourceAmount(item("gravel"), 1)));

        final long moved = new SweepTask(ResourceFilter.NONE, FOUR).runOnce(route(source, network));

        assertThat(moved).isEqualTo(4);
        assertThat(network.amountOf(DIRT)).isEqualTo(5);
        assertThat(source.amountOf(STONE)).isEqualTo(10);
    }

    @Test
    void pullsNothingFromAnEmptySource() {
        final long moved = new SweepTask(ResourceFilter.NONE, FOUR).runOnce(route(empty(), empty()));

        assertThat(moved).isZero();
    }

    @Test
    void pullsNothingTheFilterDenies() {
        final CellStorage source = chest(new ResourceAmount(STONE, 10));

        final long moved = new SweepTask(new ResourceFilter(FilterMode.ALLOW, Set.of(DIRT)), FOUR)
                .runOnce(route(source, empty()));

        assertThat(moved).isZero();
        assertThat(source.amountOf(STONE)).isEqualTo(10);
    }

    private static StorageRoute route(final CellStorage source, final CellStorage network) {
        return new StorageRoute(source, network, Actor.NOBODY);
    }

    private static CellStorage chest(final ResourceAmount... contents) {
        return new CellStorage(ITEMS, SMALL, List.of(contents));
    }

    private static CellStorage empty() {
        return new CellStorage(ITEMS, SMALL, List.of());
    }
}
