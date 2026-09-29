package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A storage seen through one kind of resource: resources of other kinds are
 * neither listed, accepted nor given, as if the storage held none. A device
 * set to move fluids sees its network this way.
 */
public final class SingleTypeStorage implements Storage {

    private final Storage delegate;
    private final ResourceType type;

    /**
     * @param delegate the storage behind the view, shared with its owner and read live
     */
    public SingleTypeStorage(final Storage delegate, final ResourceType type) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        return resource.type() == type ? delegate.insert(resource, amount, action, actor) : 0;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        return resource.type() == type ? delegate.extract(resource, amount, action, actor) : 0;
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return resource.type() == type ? delegate.amountOf(resource) : 0;
    }

    /**
     * @return the resources of this kind, in the order the storage lists them; a snapshot
     */
    @Override
    public List<ResourceAmount> contents() {
        final List<ResourceAmount> held = delegate.contents();
        final List<ResourceAmount> ofType = new ArrayList<>(held.size());
        for (ResourceAmount amount : held) {
            if (amount.resource().type() == type) {
                ofType.add(amount);
            }
        }
        return List.copyOf(ofType);
    }

    @Override
    public boolean isReservedFor(final ResourceKey resource) {
        return resource.type() == type && delegate.isReservedFor(resource);
    }
}
