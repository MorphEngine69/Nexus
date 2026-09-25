package com.morphengine.nexus.network;

import com.morphengine.nexus.api.network.NetworkNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkGraphsTest {

    @Test
    void isolatedNodeReachesOnlyItself() {
        final FakeNode start = new FakeNode("a");

        final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(start, node -> List.of());

        assertThat(reachable).containsExactly(start);
    }

    @Test
    void traversalFollowsEdgesTransitively() {
        final FakeNode a = new FakeNode("a");
        final FakeNode b = new FakeNode("b");
        final FakeNode c = new FakeNode("c");
        final FakeNode isolated = new FakeNode("isolated");
        final Map<NetworkNode, List<NetworkNode>> edges = Map.of(
                a, List.of(b),
                b, List.of(a, c),
                c, List.of(b),
                isolated, List.of());

        final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(a, node -> edges.getOrDefault(node, List.of()));

        assertThat(reachable).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    void cyclesDoNotCauseInfiniteTraversal() {
        final FakeNode a = new FakeNode("a");
        final FakeNode b = new FakeNode("b");
        final Map<NetworkNode, List<NetworkNode>> edges = Map.of(
                a, List.of(b),
                b, List.of(a));

        final Set<NetworkNode> reachable = NetworkGraphs.reachableFrom(a, node -> edges.getOrDefault(node, List.of()));

        assertThat(reachable).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void rejectsNullArguments() {
        final FakeNode start = new FakeNode("a");

        assertThatThrownBy(() -> NetworkGraphs.reachableFrom(null, node -> List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> NetworkGraphs.reachableFrom(start, null))
                .isInstanceOf(NullPointerException.class);
    }

    private record FakeNode(String id) implements NetworkNode {
    }
}
