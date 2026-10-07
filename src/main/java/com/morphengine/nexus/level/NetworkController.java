package com.morphengine.nexus.level;

import com.morphengine.nexus.access.PlayerPlaced;
import com.morphengine.nexus.access.Secured;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.security.AccessGate;
import com.morphengine.nexus.security.NetworkSecurity;
import com.morphengine.nexus.storage.EnergyBackedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * The block entity that defines a network, one per network.
 */
public interface NetworkController extends Secured, PlayerPlaced {

    /**
     * Marks the network membership as outdated. Cheap; the rebuild happens later
     * on the server tick, once however many changes arrived in between.
     */
    void invalidateNetwork();

    /**
     * @return id, name and color of the network; the id stays the same when the
     *         Nexus is broken and placed again elsewhere
     */
    Network network();

    /**
     * @return the figures of the network as of the last second, among them how many devices it has. Server side only.
     */
    NetworkStatistics statistics();

    /**
     * @return the energy pool of the network as of the last rebuild: generators
     *         feed it, consumers draw from it. Server side only.
     */
    EnergyBuffer energy();

    /**
     * @return everything the network holds as one storage: the items and fluids
     *         of its storage and its whole energy pool as {@link EnergyKey}. A new
     *         view on every call, over the storage and pool of the moment. Like
     *         the storage itself, it lets an actor in or out only as far as its
     *         {@link #gate()} permits. Server side only.
     */
    default Storage resources() {
        return new EnergyBackedStorage(component(NetworkComponentTypes.STORAGE).storage(), energy(),
                EnergyKey.INSTANCE, gate());
    }

    /**
     * @return the access side of the network; see {@link NetworkGuard}. Server side only.
     */
    NetworkGuard guard();

    /**
     * @return who may do what with the network. Server side only.
     */
    default NetworkSecurity security() {
        return guard().security();
    }

    /**
     * @return what decides every insert into and extract from the network's
     *         resources by the access rights of the player its actor acts for;
     *         see {@link NetworkGuard}. Server side only.
     */
    default AccessGate gate() {
        return guard().gate();
    }

    @Override
    default AccessPolicy accessPolicy() {
        return security();
    }

    /**
     * The player who places a controller that starts a network of its own
     * becomes the network's owner.
     */
    @Override
    default void placedBy(final Player player) {
        guard().foundBy(player);
    }

    /**
     * @return the network's component of {@code type}; the same instance for the
     *         whole life of the controller. Server side only.
     */
    <C extends NetworkComponent> C component(NetworkComponentType<C> type);

    /**
     * @return whether the controller has left the level, removed or unloaded
     */
    boolean isRemoved();

    BlockPos getBlockPos();

    /**
     * @return game time at which the controller first ran; when two controllers
     *         end up in one network, the earlier one leads it
     */
    long establishedAt();

    /**
     * Shows what the network is doing on the controller's model. Called on every
     * status refresh; the controller changes its block only when needed.
     */
    void showStatus(NexusStatus status);
}
