package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.transport.SchedulingMode;
import com.morphengine.nexus.api.transport.TransferQuota;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * What one operation of a Pusher does: delivers the first of its entries,
 * tried in the order its {@link SchedulingMode} gives, that moves anything out
 * of the network into the storage beside it. A resource is delivered only up
 * to the amount it keeps there; a group delivers the first of its members the
 * network holds, in the order the network lists them, that the storage takes.
 * Where round robin goes on is remembered between operations, not saved.
 * Server thread only.
 */
public final class PushTask implements TransferTask {

    private final List<PushEntry> entries;
    private final SchedulingMode scheduling;
    private final TransferQuota quota;
    private final RandomGenerator random;
    private int next;

    /**
     * @param entries what to deliver, in the order of the filter; copied
     * @param random  picks the first entry tried under {@link SchedulingMode#RANDOM}
     */
    public PushTask(
            final List<? extends PushEntry> entries, final SchedulingMode scheduling, final TransferQuota quota,
            final RandomGenerator random) {
        this.entries = List.copyOf(entries);
        this.scheduling = Objects.requireNonNull(scheduling, "scheduling must not be null");
        this.quota = Objects.requireNonNull(quota, "quota must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * @param route from the network into the storage beside the device
     */
    @Override
    public long runOnce(final StorageRoute route) {
        final int size = entries.size();
        if (size == 0) {
            return 0;
        }
        final int start = switch (scheduling) {
            case IN_ORDER -> 0;
            case ROUND_ROBIN -> next % size;
            case RANDOM -> random.nextInt(size);
        };
        for (int step = 0; step < size; step++) {
            final int index = (start + step) % size;
            final long moved = deliver(entries.get(index), route);
            if (moved > 0) {
                next = index + 1;
                return moved;
            }
        }
        return 0;
    }

    private long deliver(final PushEntry entry, final StorageRoute route) {
        return switch (entry) {
            case StockEntry stock -> deliverStock(stock, route);
            case GroupEntry group -> deliverGroup(group, route);
        };
    }

    private long deliverStock(final StockEntry entry, final StorageRoute route) {
        final long missing = entry.keep() - route.delivered(entry.resource());
        final long wanted = Math.min(quota.unitsPerOperation(entry.resource()), missing);
        return wanted > 0 ? route.move(entry.resource(), wanted) : 0;
    }

    private long deliverGroup(final GroupEntry entry, final StorageRoute route) {
        final List<ResourceAmount> offered = route.offered();
        for (int i = 0; i < offered.size(); i++) {
            final ResourceAmount held = offered.get(i);
            if (entry.group().contains(held.resource())) {
                final long moved = route.move(held.resource(), quota.unitsPerOperation(held.resource()));
                if (moved > 0) {
                    return moved;
                }
            }
        }
        return 0;
    }
}
