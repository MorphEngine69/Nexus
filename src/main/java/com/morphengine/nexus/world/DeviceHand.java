package com.morphengine.nexus.world;

import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * What a Placer or Remover works with in the space before it: the tool it
 * breaks with, and the player it works for.
 *
 * @param owner the player the device works for; {@code null} for a device nobody owns
 */
public record DeviceHand(HarvestTool tool, @Nullable NameAndId owner) {

    public DeviceHand {
        Objects.requireNonNull(tool, "tool must not be null");
    }
}
