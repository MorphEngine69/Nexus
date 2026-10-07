package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.resource.ResourceAmount;
import org.junit.jupiter.api.Test;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceCounterTest {

    @Test
    void emptyCounterHoldsNothing() {
        final ResourceCounter counter = new ResourceCounter();

        assertThat(counter.amountOf(STONE)).isZero();
        assertThat(counter.size()).isZero();
        assertThat(counter.total()).isZero();
        assertThat(counter.contents()).isEmpty();
    }

    @Test
    void addAccumulatesPerResourceAndInTotal() {
        final ResourceCounter counter = new ResourceCounter();

        counter.add(STONE, 10);
        final long stone = counter.add(STONE, 5);
        counter.add(DIRT, 3);

        assertThat(stone).isEqualTo(15);
        assertThat(counter.size()).isEqualTo(2);
        assertThat(counter.total()).isEqualTo(18);
    }

    @Test
    void removingEverythingForgetsTheResource() {
        final ResourceCounter counter = new ResourceCounter();
        counter.add(STONE, 10);

        final long left = counter.remove(STONE, 10);

        assertThat(left).isZero();
        assertThat(counter.size()).isZero();
        assertThat(counter.total()).isZero();
    }

    @Test
    void removingMoreThanCountedIsRejected() {
        final ResourceCounter counter = new ResourceCounter();
        counter.add(STONE, 4);

        assertThatThrownBy(() -> counter.remove(STONE, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(counter.amountOf(STONE)).isEqualTo(4);
    }

    @Test
    void contentsKeepFirstSeenOrder() {
        final ResourceCounter counter = new ResourceCounter();
        counter.add(SAND, 1);
        counter.add(STONE, 2);
        counter.add(SAND, 3);

        assertThat(counter.contents()).containsExactly(new ResourceAmount(SAND, 4), new ResourceAmount(STONE, 2));
    }

    @Test
    void overflowingLongIsRejected() {
        final ResourceCounter counter = new ResourceCounter();
        counter.add(STONE, Long.MAX_VALUE);

        assertThatThrownBy(() -> counter.add(STONE, 1)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        final ResourceCounter counter = new ResourceCounter();

        assertThatThrownBy(() -> counter.add(STONE, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.remove(STONE, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
