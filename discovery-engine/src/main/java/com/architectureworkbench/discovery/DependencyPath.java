package com.architectureworkbench.discovery;

import java.util.List;

public record DependencyPath(List<DependencyNode> nodes, List<DependencyEdge> edges, boolean truncated) {
    public DependencyPath {
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
        if (nodes.isEmpty()) throw new IllegalArgumentException("Path nodes are required.");
    }
}
