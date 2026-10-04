package com.morphengine.nexus.security;

import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.storage.Actor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.morphengine.nexus.test.TestActors.actingFor;
import static org.assertj.core.api.Assertions.assertThat;

class AccessGateTest {

    private static final UUID ALLOWED = new UUID(0, 1);
    private static final UUID REFUSED = new UUID(0, 2);
    private static final AccessPolicy POLICY = (player, permission) -> player.equals(ALLOWED);

    private final AccessGate gate = AccessGate.of(POLICY);

    @Test
    void actorOfAnAllowedPlayerPasses() {
        assertThat(gate.permits(actingFor(ALLOWED), Permission.EXTRACT)).isTrue();
    }

    @Test
    void actorOfARefusedPlayerIsTurnedAway() {
        assertThat(gate.permits(actingFor(REFUSED), Permission.EXTRACT)).isFalse();
    }

    @Test
    void actorThatActsForNoPlayerPasses() {
        assertThat(gate.permits(Actor.NOBODY, Permission.EXTRACT)).isTrue();
    }

    @Test
    void unrestrictedGateLetsEveryoneThrough() {
        assertThat(AccessGate.UNRESTRICTED.permits(actingFor(REFUSED), Permission.BUILD)).isTrue();
    }
}
