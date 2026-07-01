package com.architectureworkbench.knowledgegraphmcp;

import java.util.List;

public record GraphSnapshot(
        String graphId,
        List<ElementView> elements,
        List<RelationshipView> relationships
) {
    public GraphSnapshot {
        elements = List.copyOf(elements == null ? List.of() : elements);
        relationships = List.copyOf(relationships == null ? List.of() : relationships);
    }

    public record ElementView(String id, String type, String name, String description) {}
    public record RelationshipView(String id, String sourceId, String targetId, String type, String label) {}
}
