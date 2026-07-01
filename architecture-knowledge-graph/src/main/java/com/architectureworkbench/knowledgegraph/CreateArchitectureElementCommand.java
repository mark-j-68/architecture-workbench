package com.architectureworkbench.knowledgegraph;

import java.util.Map;
import java.util.Objects;

public record CreateArchitectureElementCommand(
        ArchitectureElementType type,
        String name,
        String description,
        Map<String, String> attributes,
        String actorRef
) {
    public CreateArchitectureElementCommand {
        type = Objects.requireNonNull(type, "type");
        attributes = Map.copyOf(Objects.requireNonNullElseGet(attributes, Map::of));
        actorRef = Objects.requireNonNullElse(actorRef, "system");
    }
}
