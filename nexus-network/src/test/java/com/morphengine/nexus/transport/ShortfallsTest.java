package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.storage.CellStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.SMALL;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class ShortfallsTest {

    private static final TransferQuota FOUR = resource -> 4;

    @Test
    void anEntryWithoutAmountLacksOneOperationsWorth() {
        final ResourceAmount lacking = Shortfalls.first(List.of(StockEntry.unlimited(STONE)), storage(),
                storage(new ResourceAmount(STONE, 1)), FOUR);

        assertThat(lacking).isEqualTo(new ResourceAmount(STONE, 3));
    }

    @Test
    void anEntryKeptInStockLacksWhatTheStorageBesideMisses() {
        final ResourceAmount lacking = Shortfalls.first(List.of(new StockEntry(STONE, 10)),
                storage(new ResourceAmount(STONE, 6)), storage(), FOUR);

        assertThat(lacking).isEqualTo(new ResourceAmount(STONE, 4));
    }

    @Test
    void nothingLacksWhileTheNetworkHoldsEnough() {
        final ResourceAmount lacking = Shortfalls.first(List.of(new StockEntry(STONE, 10)), storage(),
                storage(new ResourceAmount(STONE, 10)), FOUR);

        assertThat(lacking).isNull();
    }

    @Test
    void aFullStockLacksNothing() {
        final ResourceAmount lacking = Shortfalls.first(List.of(new StockEntry(STONE, 5)),
                storage(new ResourceAmount(STONE, 8)), storage(), FOUR);

        assertThat(lacking).isNull();
    }

    @Test
    void theFirstEntryThatLacksComesFirst() {
        final ResourceAmount lacking = Shortfalls.first(
                List.of(StockEntry.unlimited(STONE), StockEntry.unlimited(DIRT)), storage(),
                storage(new ResourceAmount(STONE, 9)), FOUR);

        assertThat(lacking).isEqualTo(new ResourceAmount(DIRT, 4));
    }

    private static CellStorage storage(final ResourceAmount... contents) {
        return new CellStorage(ITEMS, SMALL, List.of(contents));
    }
}
