package com.morphengine.nexus.level;

import com.morphengine.nexus.api.storage.Actor;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A device working on its own, such as a Pusher, for the player who placed it:
 * what it does with the network is subject to that player's access rights.
 *
 * @param owner the player the device works for; {@code null} for a device
 *              nobody owns yet, whose work counts as the network's own
 * @param name  the device's name, as shown to players
 */
public record DeviceActor(@Nullable UUID owner, String name) implements Actor {

    public DeviceActor {
        Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public @Nullable UUID player() {
        return owner;
    }
}
