package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;

import java.util.List;
import java.util.Objects;

/**
 * A storage that reports every change of its contents, so that its owner knows
 * when to save it. Simulated operations and operations that move nothing are
 * not reported.
 */
public final class ObservedStorage implements Storage {

    private final Storage delegate;
    private final Runnable onChange;

    /**
     * @param onChange run after each executed insert or extract that moved something
     */
    public ObservedStorage(final Storage delegate, final Runnable onChange) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        return reported(delegate.insert(resource, amount, action, actor), action);
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        return reported(delegate.extract(resource, amount, action, actor), action);
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
        return delegate.isReservedFor(resource);
    }

    private long reported(final long moved, final Action action) {
        if (moved > 0 && action.isExecute()) {
            onChange.run();
        }
        return moved;
    }
}
