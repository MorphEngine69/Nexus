package com.morphengine.nexus.level;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.block.NexusStatus;
import net.minecraft.core.BlockPos;

/**
 * The block entity that defines a network, one per network.
 */
public interface NetworkController {

    /**
     * Marks the network membership as outdated. Cheap; the rebuild happens later
     * on the server tick, once however many changes arrived in between.
     */
    void invalidateNetwork();

    /**
     * @return name and color of the network, shown by every device in it
     */
    Network network();

    /**
     * @return the energy pool of the network as of the last rebuild: generators
     *         feed it, consumers draw from it. Server side only.
     */
    EnergyBuffer energy();

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
