package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.RelationshipType;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GraphSnapshot(
        String graphId,
        Instant exportedAt,
        List<ElementSnapshot> elements,
        List<RelationshipSnapshot> relationships
) {
    public GraphSnapshot {
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException("Graph id is required.");
        }
        exportedAt = exportedAt == null ? Instant.now() : exportedAt;
        elements = List.copyOf(elements == null ? List.of() : elements);
        relationships = List.copyOf(relationships == null ? List.of() : relationships);
    }

    public record ElementSnapshot(
            String id,
            ArchitectureElementType type,
            String name,
            String description,
            Map<String, String> attributes
    ) {
        public ElementSnapshot {
            attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        }
    }

    public record RelationshipSnapshot(
            String id,
            String sourceId,
            String targetId,
            RelationshipType type,
            String label,
            Map<String, String> attributes
    ) {
        public RelationshipSnapshot {
            attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        }
    }
}
