package com.architectureworkbench.discovery;

public record TopologyNode(String id, String kind) {
    public TopologyNode {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required.");
        kind = kind == null ? "unknown" : kind;
    }
}
