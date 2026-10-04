package com.morphengine.nexus.test;

import com.morphengine.nexus.api.storage.Actor;

import java.util.UUID;

/**
 * Actors for tests of the core, standing in for players at terminals and their devices.
 */
public final class TestActors {

    private TestActors() {
    }

    /**
     * @return an actor that acts for {@code player}
     */
    public static Actor actingFor(final UUID player) {
        return new PlayerActor(player);
    }

    private record PlayerActor(UUID player) implements Actor {

        @Override
        public String name() {
            return player.toString();
        }
    }
}
