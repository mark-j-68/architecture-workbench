package com.architectureworkbench.knowledgegraph;

import java.util.Map;
import java.util.Objects;

public record LinkElementsCommand(
        ElementId sourceId,
        ElementId targetId,
        RelationshipType type,
        String label,
        Map<String, String> attributes,
        String actorRef
) {
    public LinkElementsCommand {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        targetId = Objects.requireNonNull(targetId, "targetId");
        type = Objects.requireNonNull(type, "type");
        attributes = Map.copyOf(Objects.requireNonNullElseGet(attributes, Map::of));
        actorRef = Objects.requireNonNullElse(actorRef, "system");
    }
}
