package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.api.storage.StorageListener;
import com.morphengine.nexus.security.AccessGate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.morphengine.nexus.test.TestActors.actingFor;
import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkStorageTest {

    private static final UUID PLAYER = new UUID(0, 1);

    private final NetworkStorage network = new NetworkStorage();
    private final List<String> heard = new ArrayList<>();
    private final StorageListener listener = (resource, amount) -> heard.add(resource + "=" + amount);

    @Test
    void storageWithoutSourcesTakesNothing() {
        assertThat(network.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(network.extract(STONE, 10, Action.EXECUTE, Actor.NOBODY)).isZero();
    }

    @Test
    void insertFillsHighestPriorityFirst() {
        final CellStorage low = cell();
        final CellStorage high = cell();
        network.addSource(low, 0);
        network.addSource(high, 5);

        network.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(high.amountOf(STONE)).isEqualTo(10);
        assertThat(low.amountOf(STONE)).isZero();
    }

    @Test
    void insertSpillsIntoLowerPriorityWhenHigherIsFull() {
        final CellStorage low = cell();
        final CellStorage high = cell();
        network.addSource(low, 0);
        network.addSource(high, 5);

        final long accepted = network.insert(STONE, 500, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(500);
        assertThat(high.amountOf(STONE)).isEqualTo(448);
        assertThat(low.amountOf(STONE)).isEqualTo(52);
        assertThat(network.amountOf(STONE)).isEqualTo(500);
    }

    @Test
    void equalPriorityPrefersSourceAlreadyHoldingTheResource() {
        final CellStorage empty = cell();
        final CellStorage holding = cell();
        holding.insert(STONE, 1, Action.EXECUTE, Actor.NOBODY);
        network.addSource(empty, 0);
        network.addSource(holding, 0);

        network.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(holding.amountOf(STONE)).isEqualTo(11);
        assertThat(empty.amountOf(STONE)).isZero();
    }

    @Test
    void equalPriorityPrefersSourceReservedForTheResource() {
        final CellStorage general = cell();
        final CellStorage stoneOnly = cell();
        network.addSource(general, 0);
        network.addSource(new FilteredStorage(stoneOnly, new ResourceFilter(FilterMode.ALLOW, Set.of(STONE))), 0);

        network.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);
        network.insert(DIRT, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(stoneOnly.amountOf(STONE)).isEqualTo(10);
        assertThat(general.amountOf(DIRT)).isEqualTo(10);
        assertThat(general.amountOf(STONE)).isZero();
    }

    @Test
    void extractEmptiesLowestPriorityFirst() {
        final CellStorage low = cellWith(STONE, 5);
        final CellStorage high = cellWith(STONE, 5);
        network.addSource(high, 5);
        network.addSource(low, 0);

        final long extracted = network.extract(STONE, 7, Action.EXECUTE, Actor.NOBODY);

        assertThat(extracted).isEqualTo(7);
        assertThat(low.amountOf(STONE)).isZero();
        assertThat(high.amountOf(STONE)).isEqualTo(3);
    }

    @Test
    void extractTakesAtMostWhatTheNetworkHolds() {
        network.addSource(cellWith(STONE, 5), 0);

        assertThat(network.extract(STONE, 50, Action.EXECUTE, Actor.NOBODY)).isEqualTo(5);
        assertThat(network.amountOf(STONE)).isZero();
    }

    @Test
    void simulationChangesNothingAndIsNotHeard() {
        network.addSource(cellWith(STONE, 5), 0);
        network.addListener(listener);

        network.insert(STONE, 10, Action.SIMULATE, Actor.NOBODY);
        network.extract(STONE, 2, Action.SIMULATE, Actor.NOBODY);

        assertThat(network.amountOf(STONE)).isEqualTo(5);
        assertThat(heard).isEmpty();
    }

    @Test
    void listenersHearNewTotals() {
        network.addSource(cell(), 0);
        network.addListener(listener);

        network.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);
        network.extract(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(heard).containsExactly(STONE + "=10", STONE + "=0");
    }

    @Test
    void addingAndRemovingSourceChangesTotals() {
        final CellStorage source = cellWith(STONE, 5);
        network.addSource(cellWith(STONE, 2), 0);
        network.addListener(listener);

        network.addSource(source, 0);
        final long withSource = network.amountOf(STONE);
        final boolean removed = network.removeSource(source);

        assertThat(withSource).isEqualTo(7);
        assertThat(removed).isTrue();
        assertThat(network.contents()).containsExactly(new ResourceAmount(STONE, 2));
        assertThat(heard).containsExactly(STONE + "=7", STONE + "=2");
    }

    @Test
    void removingUnknownSourceDoesNothing() {
        assertThat(network.removeSource(cell())).isFalse();
    }

    @Test
    void changingPriorityReordersWithoutNotifying() {
        final CellStorage first = cell();
        final CellStorage second = cell();
        network.addSource(first, 5);
        network.addSource(second, 0);
        network.addListener(listener);

        network.changePriority(second, 10);
        final List<String> heardOnReorder = List.copyOf(heard);
        network.insert(STONE, 3, Action.EXECUTE, Actor.NOBODY);

        assertThat(heardOnReorder).isEmpty();
        assertThat(second.amountOf(STONE)).isEqualTo(3);
        assertThat(first.amountOf(STONE)).isZero();
    }

    @Test
    void sameSourceCannotBeAddedTwice() {
        final CellStorage source = cell();
        network.addSource(source, 0);

        assertThatThrownBy(() -> network.addSource(source, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removedListenerHearsNothing() {
        network.addSource(cell(), 0);
        network.addListener(listener);
        network.removeListener(listener);

        network.insert(STONE, 1, Action.EXECUTE, Actor.NOBODY);

        assertThat(heard).isEmpty();
    }

    @Test
    void anInterceptorClaimsPartOfAnInsertBeforeAnySource() {
        final CellStorage source = cell();
        network.addSource(source, 0);
        network.addInterceptor((resource, amount, action) -> Math.min(amount, 3));

        final long inserted = network.insert(STONE, 5, Action.EXECUTE, Actor.NOBODY);

        assertThat(inserted).isEqualTo(5);
        assertThat(source.amountOf(STONE)).isEqualTo(2);
        assertThat(network.amountOf(STONE)).isEqualTo(2);
    }

    @Test
    void anInterceptorIsToldWhatReachedTheSources() {
        final List<Long> reached = new ArrayList<>();
        network.addSource(cell(), 0);
        network.addInterceptor(new InsertInterceptor() {
            @Override
            public long intercept(final ResourceKey resource, final long amount, final Action action) {
                return 1;
            }

            @Override
            public long inserted(final ResourceKey resource, final long amount) {
                reached.add(amount);
                return amount;
            }
        });

        network.insert(STONE, 4, Action.EXECUTE, Actor.NOBODY);
        network.insert(STONE, 4, Action.SIMULATE, Actor.NOBODY);

        assertThat(reached).containsExactly(3L);
    }

    @Test
    void whatAnInterceptorClaimsCountsAsInsertedWithoutAnySource() {
        network.addInterceptor((resource, amount, action) -> amount);

        assertThat(network.insert(STONE, 7, Action.SIMULATE, Actor.NOBODY)).isEqualTo(7);
    }

    @Test
    void prioritiesAreListedOnceEachHighestFirst() {
        network.addSource(cell(), 0);
        network.addSource(cell(), 5);
        network.addSource(cell(), 0);

        assertThat(network.priorities()).containsExactly(5, 0);
    }

    @Test
    void revisionChangesWhenSourcesChange() {
        final CellStorage source = cell();
        final int initial = network.revision();

        network.addSource(source, 0);
        final int added = network.revision();
        network.changePriority(source, 3);
        final int moved = network.revision();
        network.removeSource(source);

        assertThat(List.of(initial, added, moved, network.revision())).doesNotHaveDuplicates();
    }

    @Test
    void bandInsertsOnlyIntoSourcesOfItsPriority() {
        final CellStorage low = cell();
        final CellStorage high = cell();
        network.addSource(low, 0);
        network.addSource(high, 5);

        final long accepted = network.band(0).insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(10);
        assertThat(low.amountOf(STONE)).isEqualTo(10);
        assertThat(high.amountOf(STONE)).isZero();
    }

    @Test
    void bandChangesCountTowardsTheTotalsAndAreHeard() {
        final CellStorage source = cell();
        network.addSource(source, 2);
        network.addListener(listener);

        network.band(2).insert(STONE, 10, Action.EXECUTE, Actor.NOBODY);
        network.band(2).extract(STONE, 4, Action.EXECUTE, Actor.NOBODY);

        assertThat(network.amountOf(STONE)).isEqualTo(6);
        assertThat(heard).containsExactly(STONE + "=10", STONE + "=6");
    }

    @Test
    void bandReadsAndExtractsOnlyItsOwnSources() {
        network.addSource(cellWith(STONE, 20), 0);
        network.addSource(cellWith(STONE, 30), 5);

        assertThat(network.band(5).amountOf(STONE)).isEqualTo(30);
        assertThat(network.band(5).extract(STONE, 100, Action.EXECUTE, Actor.NOBODY)).isEqualTo(30);
        assertThat(network.amountOf(STONE)).isEqualTo(20);
    }

    @Test
    void bandOfAPriorityWithoutSourcesHoldsAndTakesNothing() {
        network.addSource(cell(), 0);

        assertThat(network.band(7).insert(STONE, 10, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(network.band(7).amountOf(STONE)).isZero();
        assertThat(network.band(7).contents()).isEmpty();
    }

    @Test
    void bandSimulationChangesNothing() {
        final CellStorage source = cell();
        network.addSource(source, 0);

        final long accepted = network.band(0).insert(STONE, 10, Action.SIMULATE, Actor.NOBODY);

        assertThat(accepted).isEqualTo(10);
        assertThat(network.amountOf(STONE)).isZero();
    }

    @Test
    void gateTurnsAwayAnInsertForAPlayerWithoutTheRight() {
        final CellStorage source = cell();
        network.addSource(source, 0);
        network.guardWith(AccessGate.of((player, permission) -> permission != Permission.INSERT));

        final long accepted = network.insert(STONE, 10, Action.EXECUTE, actingFor(PLAYER));

        assertThat(accepted).isZero();
        assertThat(source.amountOf(STONE)).isZero();
        assertThat(heardAnything()).isFalse();
    }

    @Test
    void gateTurnsAwayAnExtractForAPlayerWithoutTheRight() {
        network.addSource(cellWith(STONE, 10), 0);
        network.guardWith(AccessGate.of((player, permission) -> permission != Permission.EXTRACT));

        assertThat(network.extract(STONE, 10, Action.SIMULATE, actingFor(PLAYER))).isZero();
        assertThat(network.extract(STONE, 10, Action.EXECUTE, actingFor(PLAYER))).isZero();
        assertThat(network.amountOf(STONE)).isEqualTo(10);
    }

    @Test
    void gateLetsAPlayerWithTheRightThrough() {
        network.addSource(cell(), 0);
        network.guardWith(AccessGate.of((player, permission) -> player.equals(PLAYER)));

        assertThat(network.insert(STONE, 10, Action.EXECUTE, actingFor(PLAYER))).isEqualTo(10);
        assertThat(network.extract(STONE, 4, Action.EXECUTE, actingFor(PLAYER))).isEqualTo(4);
    }

    @Test
    void gateLetsTheNetworksOwnOperationsThrough() {
        network.addSource(cell(), 0);
        network.guardWith(AccessGate.of((player, permission) -> false));

        assertThat(network.insert(STONE, 10, Action.EXECUTE, Actor.NOBODY)).isEqualTo(10);
    }

    @Test
    void turnedAwayInsertIsNotOfferedToInterceptors() {
        network.addSource(cell(), 0);
        final List<Long> offered = new ArrayList<>();
        network.addInterceptor(new InsertInterceptor() {
            @Override
            public long intercept(final ResourceKey resource, final long amount, final Action action) {
                offered.add(amount);
                return 0;
            }

            @Override
            public long inserted(final ResourceKey resource, final long amount) {
                offered.add(amount);
                return 0;
            }
        });
        network.guardWith(AccessGate.of((player, permission) -> false));

        network.insert(STONE, 10, Action.EXECUTE, actingFor(PLAYER));

        assertThat(offered).isEmpty();
    }

    private boolean heardAnything() {
        return !heard.isEmpty();
    }

    private static CellStorage cell() {
        return new CellStorage(ITEMS, SMALL, List.of());
    }

    private static CellStorage cellWith(final ResourceKey resource, final long amount) {
        return new CellStorage(ITEMS, new CellSpec(1024, 8, 4, 8), List.of(new ResourceAmount(resource, amount)));
    }
}
