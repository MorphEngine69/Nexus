package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.api.storage.CellStatus;
import com.morphengine.nexus.api.storage.CellUsage;
import com.morphengine.nexus.api.storage.StorageCell;
import com.morphengine.nexus.math.SaturatedMath;

import java.util.List;
import java.util.Objects;

/**
 * A storage cell for a kind that has a single resource, such as energy. With
 * nothing else to make room for, the resource reserves no bytes and the cell
 * never runs out of types: all of {@code totalBytes * unitsPerByte} is its
 * capacity, and the cell is either {@link CellStatus#HAS_ROOM} or
 * {@link CellStatus#FULL}. {@link CellSpec#bytesPerType()} and
 * {@link CellSpec#maxTypes()} are not used.
 */
public final class SingleResourceCell implements StorageCell {

    private final ResourceKey resource;
    private final CellSpec spec;
    private final long capacity;
    private final ResourceCounter stored = new ResourceCounter();

    /**
     * @param resource the only resource the cell accepts
     * @param contents what the cell held when it was saved. Everything is kept,
     *                 even more than a smaller spec holds, so that no player
     *                 loses resources; such a cell accepts nothing until enough
     *                 is taken out.
     */
    public SingleResourceCell(final ResourceKey resource, final CellSpec spec, final List<ResourceAmount> contents) {
        this.resource = Objects.requireNonNull(resource, "resource must not be null");
        this.spec = Objects.requireNonNull(spec, "spec must not be null");
        this.capacity = SaturatedMath.multiply(spec.totalBytes(), spec.unitsPerByte());
        for (ResourceAmount content : contents) {
            stored.add(content.resource(), content.amount());
        }
    }

    @Override
    public ResourceType type() {
        return resource.type();
    }

    /**
     * @return units of the resource the cell holds at most
     */
    public long capacity() {
        return capacity;
    }

    @Override
    public long amountOf(final ResourceKey wanted) {
        return stored.amountOf(wanted);
    }

    @Override
    public List<ResourceAmount> contents() {
        return stored.contents();
    }

    @Override
    public long insert(final ResourceKey offered, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(offered, amount, action, actor);
        if (!offered.equals(resource)) {
            return 0;
        }
        final long accepted = Math.min(amount, Math.max(0, capacity - stored.total()));
        if (accepted > 0 && action.isExecute()) {
            stored.add(offered, accepted);
        }
        return accepted;
    }

    @Override
    public long extract(final ResourceKey wanted, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(wanted, amount, action, actor);
        final long removed = Math.min(amount, stored.amountOf(wanted));
        if (removed > 0 && action.isExecute()) {
            stored.remove(wanted, removed);
        }
        return removed;
    }

    /**
     * Contents over capacity, possible only when restored under a smaller spec,
     * are shown as a full cell rather than rejected.
     */
    @Override
    public CellUsage usage() {
        final long total = stored.total();
        final long usedBytes = Math.ceilDiv(total, spec.unitsPerByte());
        final CellStatus status = total >= capacity ? CellStatus.FULL : CellStatus.HAS_ROOM;
        return new CellUsage(Math.min(usedBytes, spec.totalBytes()), spec.totalBytes(), Math.min(stored.size(), 1),
                1, status);
    }
}
