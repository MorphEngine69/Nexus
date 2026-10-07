package com.morphengine.nexus.menu;

import org.jspecify.annotations.Nullable;

/**
 * A menu whose panel shows the network of its device, sent by {@link NetworkBadgeSync}.
 */
public interface NetworkBadgeView {

    /**
     * @return the badge last received; {@code null} when the device is in no network
     */
    @Nullable NetworkBadge badge();

    void acceptBadge(@Nullable NetworkBadge received);
}
