package com.morphengine.nexus.level;

import com.morphengine.nexus.access.NameAndId;
import com.morphengine.nexus.access.NetworkSecurityData;
import com.morphengine.nexus.access.Operators;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.security.AccessGate;
import com.morphengine.nexus.security.NetworkSecurity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The access side of one Nexus: whose network it leads and who may do what
 * with it, and the gate the network's resources go through. A Nexus keeps one.
 *
 * <p>When the Nexus first runs, it takes up the access of its network from the
 * world's {@link NetworkSecurityData}. A network that has none yet gets one:
 * owned by the player who placed the Nexus when the Nexus starts a network of
 * its own, unclaimed when it goes on with a network from before access rules,
 * as a Nexus loaded from an old save or placed from an old item does.
 *
 * <p>The gate turns away what the {@link Actor#player() player} an actor acts
 * for may not do; a player at a terminal who is an {@linkplain Operators
 * operator} always passes. Server thread only.
 */
public final class NetworkGuard {

    private final AccessGate gate = this::permits;
    private final Runnable establishment;
    private @Nullable NameAndId founder;
    private @Nullable UUID carried;
    private @Nullable MinecraftServer server;
    private @Nullable NetworkSecurity security;

    /**
     * @param establishment has the Nexus take up its network's access, by
     *                      {@link #establish}, when it is asked for before the
     *                      Nexus first ran
     */
    public NetworkGuard(final Runnable establishment) {
        this.establishment = establishment;
    }

    /**
     * Notes who placed the Nexus, for when it starts a network of its own.
     */
    public void foundBy(final Player player) {
        founder = NameAndId.of(player);
    }

    /**
     * Notes the network the item the Nexus was placed from carries, if any.
     */
    public void carry(final @Nullable UUID network) {
        carried = network;
    }

    public boolean isEstablished() {
        return security != null;
    }

    /**
     * Records the Nexus at {@code pos} as the one of {@code network}, or of a new
     * network if another Nexus leads that one still, and takes up the access of
     * the network it ends up leading.
     *
     * @return the id of the network the Nexus leads
     */
    public UUID establish(final ServerLevel level, final BlockPos pos, final UUID network) {
        final MinecraftServer at = level.getServer();
        final UUID claimed = NetworkDirectory.of(at).claim(network, GlobalPos.of(level.dimension(), pos), at);
        final boolean inherited = claimed.equals(carried);
        security = NetworkSecurityData.of(at).establish(claimed, inherited ? null : founder);
        server = at;
        founder = null;
        return claimed;
    }

    /**
     * Takes the access up on the spot when asked before the Nexus first ran.
     *
     * @throws IllegalStateException on the client, or for a Nexus outside any level
     */
    public NetworkSecurity security() {
        if (security == null) {
            establishment.run();
        }
        final NetworkSecurity current = security;
        if (current == null) {
            throw new IllegalStateException("network access is known on the server only, to a Nexus in a level");
        }
        return current;
    }

    public AccessGate gate() {
        return gate;
    }

    /**
     * For a Nexus just placed from an item that carries a network: the rules of
     * that network, which the Nexus goes on with unless another Nexus still
     * leads it, so that only those who may manage the network can move it.
     *
     * @return the rules of the carried network; {@code null} when the Nexus
     *         starts a network of its own or the network has no rules yet
     */
    public @Nullable AccessPolicy inheritedPolicy(final ServerLevel level, final BlockPos pos) {
        final UUID network = carried;
        final MinecraftServer at = level.getServer();
        if (network == null || NetworkDirectory.of(at).isLeadElsewhere(network, GlobalPos.of(level.dimension(), pos),
                at)) {
            return null;
        }
        return NetworkSecurityData.of(at).find(network);
    }

    private boolean permits(final Actor actor, final Permission permission) {
        final UUID player = actor.player();
        if (player == null || security().isAllowed(player, permission)) {
            return true;
        }
        return actor instanceof PlayerActor && isOperatorOnline(player);
    }

    private boolean isOperatorOnline(final UUID player) {
        final MinecraftServer current = server;
        final ServerPlayer online = current != null ? current.getPlayerList().getPlayer(player) : null;
        return online != null && Operators.isOperator(online);
    }
}
