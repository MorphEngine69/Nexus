package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;

/**
 * Sees every insert into a {@link NetworkStorage} and may claim part of it
 * before it reaches any storage, such as a crafting task taking back the
 * outputs it waits for. Server thread only.
 */
public interface InsertInterceptor {

    /**
     * Claims part of an insert before it reaches the storages.
     *
     * @param amount units offered, always positive
     * @return units claimed, from zero to {@code amount}; under
     *         {@link Action#SIMULATE} the units that would be claimed, with
     *         nothing changed
     */
    long intercept(ResourceKey resource, long amount, Action action);

    /**
     * Told after units nobody claimed went into the storages. Interceptors
     * after this one are told only of the units it did not count.
     *
     * @param amount units that went in and nobody before counted, always positive
     * @return units this interceptor counts as its own, from zero to {@code amount}
     */
    default long inserted(final ResourceKey resource, final long amount) {
        return 0;
    }
}
