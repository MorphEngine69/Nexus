package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkLink;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Name and color of the network a device belongs to, as shown in its panel.
 */
public record NetworkBadge(String name, NetworkColor color) {

    public NetworkBadge {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(color, "color must not be null");
    }

    /**
     * @return the badge of the network {@code link} leads to; {@code null} when no
     *         Nexus is connected
     */
    public static @Nullable NetworkBadge of(final NetworkLink link) {
        return of(link.controller());
    }

    /**
     * @return the badge of the network {@code controller} leads; {@code null} when there is none
     */
    public static @Nullable NetworkBadge of(final @Nullable NetworkController controller) {
        if (controller == null) {
            return null;
        }
        final Network network = controller.network();
        return new NetworkBadge(network.name(), network.color());
    }
}
