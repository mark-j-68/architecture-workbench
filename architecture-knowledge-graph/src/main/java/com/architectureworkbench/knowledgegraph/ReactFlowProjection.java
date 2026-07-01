package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record ReactFlowProjection(
        List<Node> nodes,
        List<Edge> edges
) implements ProjectionPayload {
    public ReactFlowProjection {
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
    }

    public record Node(String id, String type, String label) {}
    public record Edge(String id, String sourceId, String targetId, String relationship, String label) {}
}
