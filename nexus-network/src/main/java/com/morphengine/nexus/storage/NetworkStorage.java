package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.api.storage.StorageListener;
import com.morphengine.nexus.math.SaturatedMath;
import com.morphengine.nexus.security.AccessGate;

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
 * of it. Listeners hear about every change. {@linkplain InsertInterceptor
 * Interceptors} see every insert first and may claim part of it; what they
 * claim counts as inserted but never reaches a source.
 *
 * <p>A {@linkplain #guardWith gate} decides every insert and extract first, by
 * who it is for: one turned away moves nothing.
 *
 * <p>A {@linkplain #addDiscarder discarder} turns the resources it lists away from
 * the sources: what is offered of them is counted as inserted and destroyed, apart
 * from what the sources {@linkplain Storage#isReservedFor reserved} for the
 * resource take, which they get first. Only inserts are affected; what the sources
 * already hold stays.
 *
 * <p>The sources of one priority can also be reached as a {@linkplain #band
 * band}, for a network that ranks them together with buffers of its own, as it
 * does with energy. Server thread only.
 */
public final class NetworkStorage implements Storage {

    private final PrioritizedSources sources = new PrioritizedSources();
    private final ResourceCounter totals = new ResourceCounter();
    private final List<StorageListener> listeners = new ArrayList<>();
    private final List<InsertInterceptor> interceptors = new ArrayList<>();
    private final List<ResourceFilter> discarders = new ArrayList<>();
    private AccessGate gate = AccessGate.UNRESTRICTED;
    private int revision;

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
        if (sources.indexOf(storage) >= 0) {
            throw new IllegalArgumentException("storage is already a source: " + storage);
        }
        sources.insert(storage, priority);
        revision++;
        for (ResourceAmount content : storage.contents()) {
            notifyListeners(content.resource(), totals.add(content.resource(), content.amount()));
        }
    }

    /**
     * @return whether the storage was a source and has been removed
     */
    public boolean removeSource(final Storage storage) {
        final int index = sources.indexOf(storage);
        if (index < 0) {
            return false;
        }
        sources.remove(index);
        revision++;
        for (ResourceAmount content : storage.contents()) {
            notifyListeners(content.resource(), totals.remove(content.resource(), content.amount()));
        }
        return true;
    }

    /**
     * Counts a change in a source that did not go through this storage, such as a hopper emptying a chest that is
     * a source, and tells the listeners. A loss larger than what the totals hold is cut down to it.
     *
     * @param delta units gained, or lost when negative
     * @return whether the storage is a source, and so the change was counted
     */
    public boolean sourceChanged(final Storage source, final ResourceKey resource, final long delta) {
        Objects.requireNonNull(resource, "resource must not be null");
        if (sources.indexOf(source) < 0) {
            return false;
        }
        if (delta > 0) {
            notifyListeners(resource, totals.add(resource, delta));
        } else if (delta < 0) {
            final long lost = Math.min(-delta, totals.amountOf(resource));
            if (lost > 0) {
                notifyListeners(resource, totals.remove(resource, lost));
            }
        }
        return true;
    }

    /**
     * Moves a source to another priority without touching the totals.
     *
     * @return whether the storage is a source
     */
    public boolean changePriority(final Storage storage, final int priority) {
        final int index = sources.indexOf(storage);
        if (index < 0) {
            return false;
        }
        sources.remove(index);
        sources.insert(storage, priority);
        revision++;
        return true;
    }

    public int sourceCount() {
        return sources.size();
    }

    /**
     * @return a number that changes whenever a source is added, removed or
     *         moved to another priority
     */
    public int revision() {
        return revision;
    }

    /**
     * @return the distinct priorities of the sources, highest first
     */
    public List<Integer> priorities() {
        final List<Integer> priorities = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            final int priority = sources.get(i).priority();
            if (priorities.isEmpty() || priorities.getLast() != priority) {
                priorities.add(priority);
            }
        }
        return List.copyOf(priorities);
    }

    /**
     * The sources of {@code priority} seen as one storage. What goes in or out
     * through it counts towards the totals and is heard by the listeners like
     * any other change, but inserts are not offered to the interceptors: a band
     * is for a network moving a resource between its own buffers, such as
     * energy. A band of a priority no source has holds and takes nothing.
     */
    public Storage band(final int priority) {
        return new Band(priority);
    }

    public void addListener(final StorageListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public void removeListener(final StorageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Lets {@code interceptor} see every insert from now on, after those added before it.
     */
    public void addInterceptor(final InsertInterceptor interceptor) {
        interceptors.add(Objects.requireNonNull(interceptor, "interceptor must not be null"));
    }

    /**
     * Has every insert of a resource {@code listing} allows destroyed from now on, as the class description says.
     * Callers keep a filter that lists something: one that lists nothing allows everything.
     */
    public void addDiscarder(final ResourceFilter listing) {
        discarders.add(Objects.requireNonNull(listing, "listing must not be null"));
    }

    /**
     * @return whether {@code listing} was added, by identity, and has been removed
     */
    public boolean removeDiscarder(final ResourceFilter listing) {
        for (int i = 0; i < discarders.size(); i++) {
            if (discarders.get(i) == listing) {
                discarders.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Has {@code newGate} decide every insert, by {@link Permission#INSERT}, and
     * every extract, by {@link Permission#EXTRACT}, from now on, in place of the
     * gate so far; until then everything passes. Bands are not guarded: they
     * are for the network's own buffers.
     */
    public void guardWith(final AccessGate newGate) {
        gate = Objects.requireNonNull(newGate, "gate must not be null");
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
        if (!gate.permits(actor, Permission.INSERT)) {
            return 0;
        }
        long claimed = 0;
        for (int i = 0; i < interceptors.size() && claimed < amount; i++) {
            claimed += interceptors.get(i).intercept(resource, amount - claimed, action);
        }
        final long offered = amount - claimed;
        final boolean discarding = offered > 0 && isDiscarded(resource);
        final long remaining = discarding
                ? insertIntoReserved(resource, offered, action, actor)
                : insertByPriority(resource, offered, action, actor);
        final long discarded = discarding ? remaining : 0;
        final long inserted = offered - remaining;
        if (inserted > 0 && action.isExecute()) {
            notifyListeners(resource, totals.add(resource, inserted));
            long uncounted = inserted;
            for (int i = 0; i < interceptors.size() && uncounted > 0; i++) {
                uncounted -= interceptors.get(i).inserted(resource, uncounted);
            }
        }
        return claimed + inserted + discarded;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        if (!gate.permits(actor, Permission.EXTRACT)) {
            return 0;
        }
        long remaining = Math.min(amount, totals.amountOf(resource));
        final long wanted = remaining;
        int end = sources.size();
        while (end > 0 && remaining > 0) {
            final int start = sources.groupStart(end);
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

    private long insertByPriority(
            final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        long remaining = amount;
        int start = 0;
        while (start < sources.size() && remaining > 0) {
            final int end = sources.groupEnd(start);
            remaining = insertIntoGroup(start, end, resource, remaining, action, actor);
            start = end;
        }
        return remaining;
    }

    private long insertIntoReserved(
            final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        long remaining = amount;
        for (int i = 0; i < sources.size() && remaining > 0; i++) {
            final Storage storage = sources.get(i).storage();
            if (storage.isReservedFor(resource)) {
                remaining -= storage.insert(resource, remaining, action, actor);
            }
        }
        return remaining;
    }

    private boolean isDiscarded(final ResourceKey resource) {
        for (int i = 0; i < discarders.size(); i++) {
            if (discarders.get(i).allows(resource)) {
                return true;
            }
        }
        return false;
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

    private void notifyListeners(final ResourceKey resource, final long amount) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAmountChanged(resource, amount);
        }
    }

    /** The sources of one priority; see {@link #band}. */
    private final class Band implements Storage {

        private final int priority;

        Band(final int priority) {
            this.priority = priority;
        }

        @Override
        public long amountOf(final ResourceKey resource) {
            final int start = sources.bandStart(priority);
            final int end = sources.bandEnd(start, priority);
            long amount = 0;
            for (int i = start; i < end; i++) {
                amount = SaturatedMath.add(amount, sources.get(i).storage().amountOf(resource));
            }
            return amount;
        }

        @Override
        public List<ResourceAmount> contents() {
            final ResourceCounter counter = new ResourceCounter();
            final int start = sources.bandStart(priority);
            final int end = sources.bandEnd(start, priority);
            for (int i = start; i < end; i++) {
                for (ResourceAmount content : sources.get(i).storage().contents()) {
                    counter.add(content.resource(), content.amount());
                }
            }
            return counter.contents();
        }

        @Override
        public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
            StorageArguments.check(resource, amount, action, actor);
            final int start = sources.bandStart(priority);
            final int end = sources.bandEnd(start, priority);
            final long inserted = amount - insertIntoGroup(start, end, resource, amount, action, actor);
            if (inserted > 0 && action.isExecute()) {
                notifyListeners(resource, totals.add(resource, inserted));
            }
            return inserted;
        }

        @Override
        public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
            StorageArguments.check(resource, amount, action, actor);
            final int start = sources.bandStart(priority);
            final int end = sources.bandEnd(start, priority);
            long remaining = amount;
            for (int i = start; i < end && remaining > 0; i++) {
                remaining -= sources.get(i).storage().extract(resource, remaining, action, actor);
            }
            final long extracted = amount - remaining;
            if (extracted > 0 && action.isExecute()) {
                notifyListeners(resource, totals.remove(resource, extracted));
            }
            return extracted;
        }
    }
}
