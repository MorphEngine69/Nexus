package com.morphengine.nexus.level;

import com.morphengine.nexus.api.storage.Actor;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * A player working with the network, such as at a terminal.
 */
public record PlayerActor(UUID id, String name) implements Actor {

    public PlayerActor {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }

    public static PlayerActor of(final Player player) {
        return new PlayerActor(player.getUUID(), player.getName().getString());
    }
}
