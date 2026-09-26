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

import java.util.List;
import java.util.Objects;

/**
 * A storage cell in the byte model: {@code types * bytesPerType} bytes are
 * reserved for the distinct resources held, and the remaining bytes hold
 * {@code unitsPerByte} units each, shared by all of them.
 */
public final class CellStorage implements StorageCell {

    private final ResourceType type;
    private final CellSpec spec;
    private final ResourceCounter stored = new ResourceCounter();

    /**
     * @param contents what the cell held when it was saved. Everything is kept,
     *                 even contents a smaller spec would not fit, so that no
     *                 player loses resources; such a cell accepts nothing until
     *                 enough is taken out.
     */
    public CellStorage(final ResourceType type, final CellSpec spec, final List<ResourceAmount> contents) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.spec = Objects.requireNonNull(spec, "spec must not be null");
        for (ResourceAmount content : contents) {
            stored.add(content.resource(), content.amount());
        }
    }

    @Override
    public ResourceType type() {
        return type;
    }

    public CellSpec spec() {
        return spec;
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return stored.amountOf(resource);
    }

    @Override
    public List<ResourceAmount> contents() {
        return stored.contents();
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        if (resource.type() != type) {
            return 0;
        }
        final long accepted = Math.min(amount, roomFor(resource));
        if (accepted > 0 && action.isExecute()) {
            stored.add(resource, accepted);
        }
        return accepted;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        final long removed = Math.min(amount, stored.amountOf(resource));
        if (removed > 0 && action.isExecute()) {
            stored.remove(resource, removed);
        }
        return removed;
    }

    /**
     * Figures over capacity, possible only for contents restored under a smaller
     * spec, are shown as a full cell rather than rejected.
     */
    @Override
    public CellUsage usage() {
        final long unitBytes = ceilDiv(stored.total(), spec.unitsPerByte());
        final long usedBytes = saturatedAdd(saturatedMultiply(stored.size(), spec.bytesPerType()), unitBytes);
        final boolean typesFull = stored.size() >= spec.maxTypes();
        final boolean bytesFull = stored.size() > 0 && unitsFreeFor(stored.size()) == 0;
        return new CellUsage(Math.min(usedBytes, spec.totalBytes()), spec.totalBytes(),
                Math.min(stored.size(), spec.maxTypes()), spec.maxTypes(), CellStatus.of(typesFull, bytesFull));
    }

    private long roomFor(final ResourceKey resource) {
        final boolean known = stored.amountOf(resource) > 0;
        final int typesAfter = stored.size() + (known ? 0 : 1);
        if (typesAfter > spec.maxTypes()) {
            return 0;
        }
        return unitsFreeFor(typesAfter);
    }

    /**
     * @return units that still fit once {@code types} resources have their bytes reserved
     */
    private long unitsFreeFor(final int types) {
        final long bytesForUnits = spec.totalBytes() - saturatedMultiply(types, spec.bytesPerType());
        if (bytesForUnits <= 0) {
            return 0;
        }
        return Math.max(0, saturatedMultiply(bytesForUnits, spec.unitsPerByte()) - stored.total());
    }

    private static long ceilDiv(final long dividend, final long divisor) {
        return dividend == 0 ? 0 : (dividend - 1) / divisor + 1;
    }

    private static long saturatedMultiply(final long left, final long right) {
        final long high = Math.multiplyHigh(left, right);
        final long product = left * right;
        return high == 0 && product >= 0 ? product : Long.MAX_VALUE;
    }

    private static long saturatedAdd(final long left, final long right) {
        final long sum = left + right;
        return sum < left ? Long.MAX_VALUE : sum;
    }
}
