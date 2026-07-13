package com.architectureworkbench.discovery;

import java.util.List;

public record DependencyCycle(List<DependencyNode> members, List<DependencyEdge> edges, boolean truncated) {
    public DependencyCycle {
        members = List.copyOf(members == null ? List.of() : members);
        edges = List.copyOf(edges == null ? List.of() : edges);
        if (members.isEmpty() || edges.isEmpty()) throw new IllegalArgumentException("Cycle members and edges are required.");
    }
}
