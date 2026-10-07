package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.StorageView;
import com.morphengine.nexus.api.transport.TransferQuota;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a Pusher would deliver and its network lacks, so that it can have it
 * crafted.
 */
public final class Shortfalls {

    private Shortfalls() {
    }

    /**
     * @param entries what the Pusher delivers, in the order it tries them
     * @param beside  the storage the Pusher delivers to
     * @param network the storage it delivers from
     * @param quota   what one operation moves, what a Pusher without an
     *                amount to keep asks for at a time
     * @return the first entry the network lacks, with how much it lacks: what
     *         is missing up to the amount kept beside, or one operation's worth
     *         for an entry without one; {@code null} when it lacks nothing
     */
    public static @Nullable ResourceAmount first(
            final List<StockEntry> entries, final StorageView beside, final StorageView network,
            final TransferQuota quota) {
        for (StockEntry entry : entries) {
            final long wanted = entry.keep() == StockEntry.UNLIMITED
                    ? quota.unitsPerOperation(entry.resource())
                    : entry.keep() - beside.amountOf(entry.resource());
            final long lacking = wanted - network.amountOf(entry.resource());
            if (lacking > 0) {
                return new ResourceAmount(entry.resource(), lacking);
            }
        }
        return null;
    }
}
