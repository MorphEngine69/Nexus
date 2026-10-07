package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.storage.CellStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class PullTaskTest {

    private static final TransferQuota FOUR = resource -> 4;

    @Test
    void takesOnlyTheSurplusAboveWhatIsKept() {
        final CellStorage chest = chest(new ResourceAmount(STONE, 6));
        final CellStorage network = empty();
        final PullTask task = new PullTask(List.of(new StockEntry(STONE, 5)), FOUR);

        final long moved = task.runOnce(new StorageRoute(chest, network, Actor.NOBODY));

        assertThat(moved).isEqualTo(1);
        assertThat(chest.amountOf(STONE)).isEqualTo(5);
    }

    @Test
    void takesAtMostTheQuota() {
        final CellStorage chest = chest(new ResourceAmount(STONE, 50));
        final PullTask task = new PullTask(List.of(new StockEntry(STONE, 5)), FOUR);

        final long moved = task.runOnce(new StorageRoute(chest, empty(), Actor.NOBODY));

        assertThat(moved).isEqualTo(4);
    }

    @Test
    void neverEmptiesBelowWhatIsKept() {
        final CellStorage chest = chest(new ResourceAmount(STONE, 5));
        final PullTask task = new PullTask(List.of(new StockEntry(STONE, 5)), FOUR);

        final long moved = task.runOnce(new StorageRoute(chest, empty(), Actor.NOBODY));

        assertThat(moved).isZero();
        assertThat(chest.amountOf(STONE)).isEqualTo(5);
    }

    @Test
    void goesOnToTheNextEntryWhenTheFirstHasNoSurplus() {
        final CellStorage chest = chest(new ResourceAmount(STONE, 2), new ResourceAmount(DIRT, 9));
        final CellStorage network = empty();
        final PullTask task = new PullTask(List.of(new StockEntry(STONE, 5), new StockEntry(DIRT, 7)), FOUR);

        final long moved = task.runOnce(new StorageRoute(chest, network, Actor.NOBODY));

        assertThat(moved).isEqualTo(2);
        assertThat(network.amountOf(DIRT)).isEqualTo(2);
        assertThat(network.amountOf(STONE)).isZero();
    }

    @Test
    void withoutEntriesItTakesNothing() {
        final CellStorage chest = chest(new ResourceAmount(STONE, 9));

        final long moved = new PullTask(List.of(), FOUR).runOnce(new StorageRoute(chest, empty(), Actor.NOBODY));

        assertThat(moved).isZero();
    }

    private static CellStorage chest(final ResourceAmount... contents) {
        return new CellStorage(ITEMS, SMALL, List.of(contents));
    }

    private static CellStorage empty() {
        return chest();
    }
}
