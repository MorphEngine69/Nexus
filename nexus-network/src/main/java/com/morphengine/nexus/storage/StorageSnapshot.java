package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.StorageView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What a storage held at the moment of the copy. It never changes afterwards, so any thread may read it while the
 * storage it came from goes on changing.
 */
public final class StorageSnapshot implements StorageView {

    private final Map<ResourceKey, Long> amounts;
    private final List<ResourceAmount> contents;

    private StorageSnapshot(final Map<ResourceKey, Long> amounts, final List<ResourceAmount> contents) {
        this.amounts = amounts;
        this.contents = contents;
    }

    /**
     * Copies what {@code storage} holds now. Reads the storage, so it is called on the thread that owns it.
     */
    public static StorageSnapshot of(final StorageView storage) {
        Objects.requireNonNull(storage, "storage must not be null");
        final List<ResourceAmount> held = storage.contents();
        final Map<ResourceKey, Long> amounts = HashMap.newHashMap(held.size());
        for (ResourceAmount amount : held) {
            amounts.merge(amount.resource(), amount.amount(), Long::sum);
        }
        return new StorageSnapshot(Collections.unmodifiableMap(amounts), held);
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return amounts.getOrDefault(resource, 0L);
    }

    @Override
    public List<ResourceAmount> contents() {
        return contents;
    }
}
