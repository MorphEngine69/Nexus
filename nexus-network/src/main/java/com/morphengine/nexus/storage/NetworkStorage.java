package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.api.storage.StorageListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Every storage of one network seen as a single storage.
 *
 * <p>Inserts fill sources from the highest priority down. Among sources of equal
 * priority, those that already hold the resource or are
 * {@linkplain Storage#isReservedFor reserved} for it come first, so like
 * resources gather in one place. Extracts empty sources from the lowest priority
 * up, so a storage given a high priority keeps its contents longest.
 *
 * <p>The sum over all sources is kept up to date, so reading an amount is cheap;
 * this relies on sources changing only through this storage while they are part
 * of it. Listeners hear about every change. Server thread only.
 */
public final class NetworkStorage implements Storage {

    /** Sorted by priority, highest first; sources of equal priority in the order they were added. */
    private final List<Source> sources = new ArrayList<>();
    private final ResourceCounter totals = new ResourceCounter();
    private final List<StorageListener> listeners = new ArrayList<>();

    /**
     * Adds a source; its contents count towards the totals from now on.
     *
     * @param storage  shared with its owner, who must not change it outside this storage
     *                 while it is a source
     * @param priority higher priorities are filled first and emptied last
     * @throws IllegalArgumentException if the storage is already a source
     */
    public void addSource(final Storage storage, final int priority) {
        Objects.requireNonNull(storage, "storage must not be null");
        if (indexOf(storage) >= 0) {
            throw new IllegalArgumentException("storage is already a source: " + storage);
        }
        insertSorted(new Source(storage, priority));
        for (ResourceAmount content : storage.contents()) {
            notifyListeners(content.resource(), totals.add(content.resource(), content.amount()));
        }
    }

    /**
     * @return whether the storage was a source and has been removed
     */
    public boolean removeSource(final Storage storage) {
        final int index = indexOf(storage);
        if (index < 0) {
            return false;
        }
        sources.remove(index);
        for (ResourceAmount content : storage.contents()) {
            notifyListeners(content.resource(), totals.remove(content.resource(), content.amount()));
        }
        return true;
    }

    /**
     * Moves a source to another priority without touching the totals.
     *
     * @return whether the storage is a source
     */
    public boolean changePriority(final Storage storage, final int priority) {
        final int index = indexOf(storage);
        if (index < 0) {
            return false;
        }
        sources.remove(index);
        insertSorted(new Source(storage, priority));
        return true;
    }

    public int sourceCount() {
        return sources.size();
    }

    public void addListener(final StorageListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public void removeListener(final StorageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return totals.amountOf(resource);
    }

    @Override
    public List<ResourceAmount> contents() {
        return totals.contents();
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        long remaining = amount;
        int start = 0;
        while (start < sources.size() && remaining > 0) {
            final int end = groupEnd(start);
            remaining = insertIntoGroup(start, end, resource, remaining, action, actor);
            start = end;
        }
        final long inserted = amount - remaining;
        if (inserted > 0 && action.isExecute()) {
            notifyListeners(resource, totals.add(resource, inserted));
        }
        return inserted;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        long remaining = Math.min(amount, totals.amountOf(resource));
        final long wanted = remaining;
        int end = sources.size();
        while (end > 0 && remaining > 0) {
            final int start = groupStart(end);
            for (int i = start; i < end && remaining > 0; i++) {
                remaining -= sources.get(i).storage().extract(resource, remaining, action, actor);
            }
            end = start;
        }
        final long extracted = wanted - remaining;
        if (extracted > 0 && action.isExecute()) {
            notifyListeners(resource, totals.remove(resource, extracted));
        }
        return extracted;
    }

    private long insertIntoGroup(
            final int start, final int end, final ResourceKey resource, final long amount, final Action action,
            final Actor actor) {
        long remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            final boolean wantsGatheringPlaces = pass == 0;
            for (int i = start; i < end && remaining > 0; i++) {
                final Storage storage = sources.get(i).storage();
                if (gathers(storage, resource) == wantsGatheringPlaces) {
                    remaining -= storage.insert(resource, remaining, action, actor);
                }
            }
        }
        return remaining;
    }

    private static boolean gathers(final Storage storage, final ResourceKey resource) {
        return storage.isReservedFor(resource) || storage.amountOf(resource) > 0;
    }

    /**
     * @return index just past the last source with the same priority as the one at {@code start}
     */
    private int groupEnd(final int start) {
        final int priority = sources.get(start).priority();
        int end = start + 1;
        while (end < sources.size() && sources.get(end).priority() == priority) {
            end++;
        }
        return end;
    }

    /**
     * @return index of the first source with the same priority as the one just before {@code end}
     */
    private int groupStart(final int end) {
        final int priority = sources.get(end - 1).priority();
        int start = end - 1;
        while (start > 0 && sources.get(start - 1).priority() == priority) {
            start--;
        }
        return start;
    }

    private void insertSorted(final Source source) {
        int index = 0;
        while (index < sources.size() && sources.get(index).priority() >= source.priority()) {
            index++;
        }
        sources.add(index, source);
    }

    private int indexOf(final Storage storage) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).storage() == storage) {
                return i;
            }
        }
        return -1;
    }

    private void notifyListeners(final ResourceKey resource, final long amount) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAmountChanged(resource, amount);
        }
    }

    private record Source(Storage storage, int priority) {
    }
}
