package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;

import java.util.List;
import java.util.Objects;

/**
 * The way resources go in one operation: out of {@code source}, into
 * {@code destination}, on behalf of {@code actor}. Both storages are shared
 * with their owners.
 */
public record StorageRoute(Storage source, Storage destination, Actor actor) {

    public StorageRoute {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
    }

    /**
     * @return what the source holds, in the order it lists it; a snapshot
     */
    public List<ResourceAmount> offered() {
        return source.contents();
    }

    /**
     * @return units of {@code resource} the destination already holds
     */
    public long delivered(final ResourceKey resource) {
        return destination.amountOf(resource);
    }

    /**
     * Moves as much of {@code resource}, up to {@code amount}, as the source gives
     * and the destination takes. Whatever the destination refuses after all
     * returns to the source.
     *
     * @param amount units to move at most, must be positive
     * @return units moved, from zero to {@code amount}
     */
    public long move(final ResourceKey resource, final long amount) {
        final long available = source.extract(resource, amount, Action.SIMULATE, actor);
        if (available <= 0) {
            return 0;
        }
        final long room = destination.insert(resource, available, Action.SIMULATE, actor);
        if (room <= 0) {
            return 0;
        }
        final long taken = source.extract(resource, room, Action.EXECUTE, actor);
        if (taken <= 0) {
            return 0;
        }
        final long moved = destination.insert(resource, taken, Action.EXECUTE, actor);
        if (moved < taken) {
            source.insert(resource, taken - moved, Action.EXECUTE, actor);
        }
        return moved;
    }
}
