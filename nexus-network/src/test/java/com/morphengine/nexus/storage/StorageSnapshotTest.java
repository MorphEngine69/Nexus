package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.ROOMY;
import static com.morphengine.nexus.test.TestResources.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageSnapshotTest {

    private static final ResourceKey LOG = item("log");
    private static final ResourceKey PLANKS = item("planks");

    @Test
    void holdsWhatTheStorageHeldWhenCopied() {
        final StorageSnapshot snapshot = StorageSnapshot.of(storageOf(amount(LOG, 5), amount(PLANKS, 12)));

        assertThat(snapshot.amountOf(LOG)).isEqualTo(5);
        assertThat(snapshot.amountOf(PLANKS)).isEqualTo(12);
        assertThat(snapshot.contents()).containsExactlyInAnyOrder(amount(LOG, 5), amount(PLANKS, 12));
    }

    @Test
    void doesNotFollowLaterChanges() {
        final NetworkStorage storage = storageOf(amount(LOG, 5));
        final StorageSnapshot snapshot = StorageSnapshot.of(storage);

        storage.insert(PLANKS, 7, Action.EXECUTE, Actor.NOBODY);

        assertThat(snapshot.amountOf(PLANKS)).isZero();
        assertThat(snapshot.contents()).containsExactly(amount(LOG, 5));
    }

    @Test
    void anEmptyStorageHoldsNothing() {
        final StorageSnapshot snapshot = StorageSnapshot.of(storageOf());

        assertThat(snapshot.amountOf(LOG)).isZero();
        assertThat(snapshot.contents()).isEmpty();
    }

    @Test
    void contentsCannotBeChanged() {
        final StorageSnapshot snapshot = StorageSnapshot.of(storageOf(amount(LOG, 5)));

        assertThatThrownBy(() -> snapshot.contents().add(amount(PLANKS, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsANullStorage() {
        assertThatThrownBy(() -> StorageSnapshot.of(null)).isInstanceOf(NullPointerException.class);
    }

    private static ResourceAmount amount(final ResourceKey resource, final long amount) {
        return new ResourceAmount(resource, amount);
    }

    private static NetworkStorage storageOf(final ResourceAmount... contents) {
        final NetworkStorage storage = new NetworkStorage();
        storage.addSource(new CellStorage(ITEMS, ROOMY, List.of(contents)), 0);
        return storage;
    }
}
