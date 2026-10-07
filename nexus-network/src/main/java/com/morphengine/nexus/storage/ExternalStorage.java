package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.api.transport.TransferQuota;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A place outside the network, such as a chest, seen as one of its storages. What the network knows of it is a copy of
 * what it held at the last {@link #rescan}, kept so that reading an amount costs nothing; a block that others change
 * too, with hoppers and by hand, is read again from time to time and what changed is handed on. Only resources the
 * filter allows are listed, taken out and put in, and the {@linkplain ExternalAccess access} says whether the network
 * may put anything in. A limit can keep what some actors move to so many units of a resource in a tick, which a
 * caller renews each tick with {@link #startTick}. Server thread only.
 */
public final class ExternalStorage implements Storage {

    private final Supplier<Storage> target;
    private final ResourceCounter shown = new ResourceCounter();
    private ResourceFilter filter = ResourceFilter.NONE;
    private final Map<ResourceKey, Long> moved = new HashMap<>();
    private ExternalAccess access = ExternalAccess.READ_WRITE;
    private TransferQuota perTick = resource -> Long.MAX_VALUE;
    private Predicate<Actor> limited = actor -> false;

    /**
     * @param target the block's own storage, asked for at every operation so that it follows the block; an
     *               {@linkplain #rescan} sees what it holds then
     */
    public ExternalStorage(final Supplier<Storage> target) {
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    /**
     * Sets what is allowed. The copy follows at the next {@link #rescan}, which a caller makes to have it at once.
     */
    public void configure(final ResourceFilter newFilter, final ExternalAccess newAccess) {
        this.filter = Objects.requireNonNull(newFilter, "newFilter must not be null");
        this.access = Objects.requireNonNull(newAccess, "newAccess must not be null");
    }

    /**
     * Keeps what the actors that {@code isLimited} says yes to move, in and out together, to the units of each
     * resource that {@code newQuota} gives for a tick. Everyone else is not held back.
     */
    public void limit(final TransferQuota newQuota, final Predicate<Actor> isLimited) {
        this.perTick = Objects.requireNonNull(newQuota, "newQuota must not be null");
        this.limited = Objects.requireNonNull(isLimited, "isLimited must not be null");
    }

    /**
     * Begins a tick: what was moved in the last one no longer counts against the limit.
     */
    public void startTick() {
        moved.clear();
    }

    /**
     * Reads the block again and tells {@code sink} each change since the last read, by the amount the copy changed.
     *
     * @return how many entries the block listed and how many resources changed, for a caller that reads again sooner
     *         or later according to how much there was to read and whether anything moved
     */
    public Scan rescan(final ChangeSink sink) {
        Objects.requireNonNull(sink, "sink must not be null");
        final ResourceCounter fresh = new ResourceCounter();
        int listed = 0;
        for (ResourceAmount held : target.get().contents()) {
            listed++;
            if (filter.allows(held.resource())) {
                fresh.add(held.resource(), held.amount());
            }
        }
        final Map<ResourceKey, Long> changes = new LinkedHashMap<>();
        for (ResourceAmount before : shown.contents()) {
            final long delta = fresh.amountOf(before.resource()) - before.amount();
            if (delta != 0) {
                changes.put(before.resource(), delta);
            }
        }
        for (ResourceAmount now : fresh.contents()) {
            if (shown.amountOf(now.resource()) == 0) {
                changes.put(now.resource(), now.amount());
            }
        }
        shown.clear();
        for (ResourceAmount now : fresh.contents()) {
            shown.add(now.resource(), now.amount());
        }
        changes.forEach(sink::changed);
        return new Scan(listed, changes.size());
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        final long allowed = allowance(resource, amount, actor);
        if (!access.allowsInsert() || !filter.allows(resource) || allowed <= 0) {
            return 0;
        }
        final long inserted = target.get().insert(resource, allowed, action, actor);
        if (inserted > 0 && action.isExecute()) {
            shown.add(resource, inserted);
            spend(resource, inserted, actor);
        }
        return inserted;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        final long allowed = allowance(resource, amount, actor);
        if (!filter.allows(resource) || allowed <= 0) {
            return 0;
        }
        final long extracted = target.get().extract(resource, allowed, action, actor);
        final long counted = Math.min(extracted, shown.amountOf(resource));
        if (counted > 0 && action.isExecute()) {
            shown.remove(resource, counted);
            spend(resource, extracted, actor);
        }
        return extracted;
    }

    private long allowance(final ResourceKey resource, final long wanted, final Actor actor) {
        if (!limited.test(actor)) {
            return wanted;
        }
        return Math.min(wanted, Math.max(0, perTick.unitsPerOperation(resource) - moved.getOrDefault(resource, 0L)));
    }

    private void spend(final ResourceKey resource, final long units, final Actor actor) {
        if (limited.test(actor)) {
            moved.merge(resource, units, Long::sum);
        }
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return shown.amountOf(resource);
    }

    @Override
    public List<ResourceAmount> contents() {
        return shown.contents();
    }

    @Override
    public boolean isReservedFor(final ResourceKey resource) {
        return filter.singlesOut(resource);
    }

    /**
     * Told of a change in what the block holds that the network did not make.
     */
    @FunctionalInterface
    public interface ChangeSink {

        /**
         * @param delta units gained, or lost when negative; never zero
         */
        void changed(ResourceKey resource, long delta);
    }

    /**
     * What a {@link #rescan} found.
     *
     * @param entries the entries the block listed, whether the filter allowed them or not
     * @param changes the resources whose amount differs from the last read
     */
    public record Scan(int entries, int changes) {

        public boolean hasChanged() {
            return changes > 0;
        }
    }
}
