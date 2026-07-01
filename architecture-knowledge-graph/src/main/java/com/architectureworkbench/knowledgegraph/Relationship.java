package com.architectureworkbench.knowledgegraph;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Relationship(
        String id,
        ElementId sourceId,
        ElementId targetId,
        RelationshipType type,
        String label,
        Map<String, String> attributes,
        Instant createdAt
) {
    public Relationship {
        if (id == null || id.isBlank()) {
            id = "rel-" + UUID.randomUUID();
        }
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        targetId = Objects.requireNonNull(targetId, "targetId");
        type = Objects.requireNonNull(type, "type");
        label = Objects.requireNonNullElse(label, type.name());
        attributes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNullElseGet(attributes, Map::of)));
        createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
    }
}
