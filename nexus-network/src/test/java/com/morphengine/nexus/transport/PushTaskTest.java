package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.SchedulingMode;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.storage.CellStorage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static com.morphengine.nexus.test.TestResources.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushTaskTest {

    private static final TransferQuota FOUR = resource -> 4;

    @Test
    void inOrderDeliversTheFirstEntryTheNetworkHas() {
        final CellStorage network = network();
        final CellStorage target = empty();
        final PushTask task = task(SchedulingMode.IN_ORDER, StockEntry.unlimited(item("clay")),
                StockEntry.unlimited(DIRT), StockEntry.unlimited(STONE));

        task.runOnce(route(network, target));
        task.runOnce(route(network, target));

        assertThat(target.contents()).containsExactly(new ResourceAmount(DIRT, 8));
    }

    @Test
    void roundRobinGoesOnAfterTheEntryDeliveredLast() {
        final CellStorage network = network();
        final CellStorage target = empty();
        final PushTask task = task(SchedulingMode.ROUND_ROBIN, StockEntry.unlimited(STONE),
                StockEntry.unlimited(item("clay")), StockEntry.unlimited(DIRT));

        task.runOnce(route(network, target));
        task.runOnce(route(network, target));
        task.runOnce(route(network, target));

        assertThat(target.contents()).containsExactly(
                new ResourceAmount(STONE, 8), new ResourceAmount(DIRT, 4));
    }

    @Test
    void randomStartsFromThePickedEntry() {
        final CellStorage target = empty();
        final PushTask task = new PushTask(List.of(StockEntry.unlimited(STONE), StockEntry.unlimited(DIRT),
                StockEntry.unlimited(SAND)), SchedulingMode.RANDOM, FOUR, alwaysPicking(2));

        task.runOnce(route(network(), target));

        assertThat(target.contents()).containsExactly(new ResourceAmount(SAND, 4));
    }

    @Test
    void keepsTheTargetStockedUpToTheAmount() {
        final CellStorage target = chest(new ResourceAmount(STONE, 6));
        final PushTask task = task(SchedulingMode.IN_ORDER, new StockEntry(STONE, 8));

        final long moved = task.runOnce(route(network(), target));

        assertThat(moved).isEqualTo(2);
        assertThat(target.amountOf(STONE)).isEqualTo(8);
    }

    @Test
    void deliversNothingToAFullyStockedTargetAndGoesOn() {
        final CellStorage target = chest(new ResourceAmount(STONE, 8));
        final PushTask task = task(SchedulingMode.IN_ORDER, new StockEntry(STONE, 8), StockEntry.unlimited(DIRT));

        task.runOnce(route(network(), target));

        assertThat(target.amountOf(STONE)).isEqualTo(8);
        assertThat(target.amountOf(DIRT)).isEqualTo(4);
    }

    @Test
    void deliversNothingWithoutEntries() {
        final long moved = task(SchedulingMode.ROUND_ROBIN).runOnce(route(network(), empty()));

        assertThat(moved).isZero();
    }

    @Test
    void deliversNothingTheNetworkLacks() {
        final long moved = task(SchedulingMode.IN_ORDER, StockEntry.unlimited(item("clay")))
                .runOnce(route(network(), empty()));

        assertThat(moved).isZero();
    }

    @Test
    void groupDeliversTheFirstMemberTheNetworkHolds() {
        final CellStorage target = empty();
        final PushTask task = task(SchedulingMode.IN_ORDER, new GroupEntry(group(SAND, DIRT)));

        task.runOnce(route(network(), target));

        assertThat(target.contents()).containsExactly(new ResourceAmount(DIRT, 4));
    }

    @Test
    void groupGoesOnToTheNextMemberTheTargetTakes() {
        final CellStorage network = chest(new ResourceAmount(DIRT, 2), new ResourceAmount(SAND, 100));
        final CellStorage target = empty();
        final PushTask task = task(SchedulingMode.IN_ORDER, new GroupEntry(group(SAND, DIRT)));

        task.runOnce(route(network, target));
        task.runOnce(route(network, target));

        assertThat(target.contents()).containsExactly(new ResourceAmount(DIRT, 2), new ResourceAmount(SAND, 4));
    }

    @Test
    void groupWithoutMembersInTheNetworkFallsThroughToTheNextEntry() {
        final CellStorage target = empty();
        final PushTask task = task(SchedulingMode.IN_ORDER, new GroupEntry(group(item("clay"))),
                StockEntry.unlimited(STONE));

        task.runOnce(route(network(), target));

        assertThat(target.contents()).containsExactly(new ResourceAmount(STONE, 4));
    }

    @Test
    void entryMustKeepAPositiveAmount() {
        assertThatThrownBy(() -> new StockEntry(STONE, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static ResourceGroup group(final ResourceKey... members) {
        return Set.of(members)::contains;
    }

    private static PushTask task(final SchedulingMode scheduling, final PushEntry... entries) {
        return new PushTask(List.of(entries), scheduling, FOUR, alwaysPicking(0));
    }

    private static StorageRoute route(final CellStorage network, final CellStorage target) {
        return new StorageRoute(network, target, Actor.NOBODY);
    }

    private static CellStorage network() {
        return chest(new ResourceAmount(STONE, 100), new ResourceAmount(DIRT, 100), new ResourceAmount(SAND, 100));
    }

    private static CellStorage chest(final ResourceAmount... contents) {
        return new CellStorage(ITEMS, SMALL, List.of(contents));
    }

    private static CellStorage empty() {
        return chest();
    }

    private static RandomGenerator alwaysPicking(final int index) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return index;
            }

            @Override
            public int nextInt(final int bound) {
                return index;
            }
        };
    }
}
