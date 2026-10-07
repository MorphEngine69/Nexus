package com.morphengine.nexus.level;

import com.morphengine.nexus.api.network.ConnectionProvider;
import com.morphengine.nexus.api.network.NetworkNode;
import com.morphengine.nexus.block.WirelessBlock;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every connection a network block has in the world a server runs: to the
 * network blocks touching it, in whatever dimension it stands, and between a
 * Network Transmitter and the Network Receiver it is linked to, both ways and
 * across dimensions. A link counts only while both of its ends still stand.
 * Never loads chunks: an end or neighbour in an unloaded chunk is skipped and
 * remembered, so the caller knows the traversal saw only part of the network.
 *
 * <p>Created per traversal on the server thread; not thread-safe.
 */
public final class ServerConnections implements ConnectionProvider {

    private final MinecraftServer server;
    private final WirelessLinks links;
    private final Map<ResourceKey<Level>, AdjacencyConnections> adjacency = new HashMap<>();
    private boolean reachedUnloaded;

    public ServerConnections(final MinecraftServer server) {
        this.server = server;
        this.links = WirelessLinks.of(server);
    }

    @Override
    public List<NetworkNode> connectionsOf(final NetworkNode node) {
        if (!(node instanceof BlockNode blockNode)) {
            return List.of();
        }
        final GlobalPos position = blockNode.position();
        final ServerLevel level = server.getLevel(position.dimension());
        if (level == null) {
            return List.of();
        }
        final List<NetworkNode> connected = new ArrayList<>(adjacencyIn(level).connectionsOf(node));
        if (isWirelessEnd(position)) {
            for (GlobalPos partner : links.partnersOf(position)) {
                if (isWirelessEnd(partner)) {
                    connected.add(new BlockNode(partner));
                }
            }
        }
        return connected;
    }

    /**
     * @return whether a traversal using this provider stopped at an unloaded chunk
     */
    public boolean reachedUnloaded() {
        if (reachedUnloaded) {
            return true;
        }
        for (AdjacencyConnections connections : adjacency.values()) {
            if (connections.reachedUnloaded()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the level a node stands in; {@code null} when that dimension is not loaded
     */
    public @Nullable ServerLevel levelOf(final BlockNode node) {
        return server.getLevel(node.position().dimension());
    }

    private AdjacencyConnections adjacencyIn(final ServerLevel level) {
        return adjacency.computeIfAbsent(level.dimension(), dimension -> new AdjacencyConnections(level));
    }

    private boolean isWirelessEnd(final GlobalPos position) {
        final ServerLevel level = server.getLevel(position.dimension());
        if (level == null || !level.isLoaded(position.pos())) {
            reachedUnloaded = true;
            return false;
        }
        return WirelessBlock.isLinkEnd(level.getBlockState(position.pos()));
    }
}
