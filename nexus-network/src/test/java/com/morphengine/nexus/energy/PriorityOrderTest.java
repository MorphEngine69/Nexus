package com.morphengine.nexus.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityOrderTest {

    @Test
    void fillsFromTheHighestPriorityDown() {
        final PriorityOrder<String> order = PriorityOrder.of(List.of(
                new PriorityOrder.Ranked<>("low", 0), new PriorityOrder.Ranked<>("high", 10),
                new PriorityOrder.Ranked<>("middle", 5)));

        assertThat(order.fillOrder()).containsExactly("high", "middle", "low");
    }

    @Test
    void drainsFromTheLowestPriorityUp() {
        final PriorityOrder<String> order = PriorityOrder.of(List.of(
                new PriorityOrder.Ranked<>("low", 0), new PriorityOrder.Ranked<>("high", 10),
                new PriorityOrder.Ranked<>("middle", 5)));

        assertThat(order.drainOrder()).containsExactly("low", "middle", "high");
    }

    @Test
    void itemsOfEqualPriorityKeepTheirOrderBothWays() {
        final PriorityOrder<String> order = PriorityOrder.of(List.of(
                new PriorityOrder.Ranked<>("first", 1), new PriorityOrder.Ranked<>("second", 1),
                new PriorityOrder.Ranked<>("top", 2)));

        assertThat(order.fillOrder()).containsExactly("top", "first", "second");
        assertThat(order.drainOrder()).containsExactly("first", "second", "top");
    }

    @Test
    void negativePrioritiesRankBelowZero() {
        final PriorityOrder<String> order = PriorityOrder.of(List.of(
                new PriorityOrder.Ranked<>("negative", -5), new PriorityOrder.Ranked<>("zero", 0)));

        assertThat(order.fillOrder()).containsExactly("zero", "negative");
    }

    @Test
    void inListOrderFillsAndDrainsInTheOrderGiven() {
        final PriorityOrder<String> order = PriorityOrder.inListOrder(List.of("a", "b"));

        assertThat(order.fillOrder()).containsExactly("a", "b");
        assertThat(order.drainOrder()).containsExactly("a", "b");
    }

    @Test
    void emptyOrderHasNoItems() {
        assertThat(PriorityOrder.<String>empty().fillOrder()).isEmpty();
        assertThat(PriorityOrder.of(List.<PriorityOrder.Ranked<String>>of()).drainOrder()).isEmpty();
    }
}
