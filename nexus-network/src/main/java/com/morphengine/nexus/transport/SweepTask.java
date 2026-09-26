package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.transport.TransferQuota;

import java.util.List;
import java.util.Objects;

/**
 * An operation that moves the first resource of the route's source, in the
 * order the source lists them, that the filter allows. A Puller sweeps the
 * block beside it this way; a Pusher with a blacklist sweeps its network.
 */
public record SweepTask(ResourceFilter filter, TransferQuota quota) implements TransferTask {

    public SweepTask {
        Objects.requireNonNull(filter, "filter must not be null");
        Objects.requireNonNull(quota, "quota must not be null");
    }

    @Override
    public long runOnce(final StorageRoute route) {
        final List<ResourceAmount> offered = route.offered();
        for (int i = 0; i < offered.size(); i++) {
            final ResourceAmount held = offered.get(i);
            if (filter.allows(held.resource())) {
                final long moved = route.move(held.resource(), quota.unitsPerOperation(held.resource()));
                if (moved > 0) {
                    return moved;
                }
            }
        }
        return 0;
    }
}
