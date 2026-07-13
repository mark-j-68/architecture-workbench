package com.architectureworkbench.discovery;

import java.util.List;

public record TopologyPath(List<TopologyNode> nodes, List<TopologyEdge> edges, boolean truncated) {
    public TopologyPath {
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
        if (nodes.isEmpty()) throw new IllegalArgumentException("Path nodes are required.");
    }
}
