package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.transport.TransferQuota;

import java.util.List;
import java.util.Objects;

/**
 * What one operation of a Puller that keeps stock does: takes the first of its
 * entries, in order, of which the storage beside it holds more than the entry
 * keeps, and takes only that surplus. The storage is never emptied below what
 * each entry keeps.
 *
 * @param entries what to take and how much of each to leave, in the order of
 *                the filter; copied
 */
public record PullTask(List<StockEntry> entries, TransferQuota quota) implements TransferTask {

    public PullTask {
        entries = List.copyOf(entries);
        Objects.requireNonNull(quota, "quota must not be null");
    }

    /**
     * @param route from the storage beside the device into the network
     */
    @Override
    public long runOnce(final StorageRoute route) {
        for (int i = 0; i < entries.size(); i++) {
            final StockEntry entry = entries.get(i);
            final long surplus = route.held(entry.resource()) - entry.keep();
            final long wanted = Math.min(quota.unitsPerOperation(entry.resource()), surplus);
            final long moved = wanted > 0 ? route.move(entry.resource(), wanted) : 0;
            if (moved > 0) {
                return moved;
            }
        }
        return 0;
    }
}
