package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;

import java.util.List;
import java.util.Objects;

/**
 * A storage that takes in only what its filter allows. Extraction is not
 * filtered: whatever got in before the filter changed can still be taken out.
 */
public final class FilteredStorage implements Storage {

    private final Storage delegate;
    private final ResourceFilter filter;

    /**
     * @param delegate the storage behind the filter, shared with its owner
     */
    public FilteredStorage(final Storage delegate, final ResourceFilter filter) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.filter = Objects.requireNonNull(filter, "filter must not be null");
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        return filter.allows(resource) ? delegate.insert(resource, amount, action, actor) : 0;
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

    @Override
    public boolean isReservedFor(final ResourceKey resource) {
        return filter.singlesOut(resource) || delegate.isReservedFor(resource);
    }
}
