package com.morphengine.nexus.menu;

import com.morphengine.nexus.networking.NetworkBadgePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Sends the network badge of a device to the player looking at its panel: once
 * when the panel opens, then whenever it changes, checked once a second.
 */
public final class NetworkBadgeSync {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private int ticks;
    private boolean sentOnce;
    private @Nullable NetworkBadge sent;

    public void tick(final ServerPlayer viewer, final int containerId, final @Nullable NetworkBadge current) {
        if (ticks++ % CHECK_INTERVAL_TICKS != 0 || sentOnce && Objects.equals(current, sent)) {
            return;
        }
        sent = current;
        sentOnce = true;
        PacketDistributor.sendToPlayer(viewer, new NetworkBadgePayload(containerId, current));
    }
}
