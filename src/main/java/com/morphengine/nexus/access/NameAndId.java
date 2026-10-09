package com.morphengine.nexus.access;

import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * A player known by the id the server gave and the name they had at the time.
 *
 * @param id   the unique id of the player
 * @param name the name of the player
 */
public record NameAndId(UUID id, String name) {

    public NameAndId {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }

    public static NameAndId of(final Player player) {
        return new NameAndId(player.getUUID(), player.getGameProfile().getName());
    }
}
