package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record BpmnProjection(
        List<String> commands,
        List<String> events,
        List<String> processRelationshipRefs
) implements ProjectionPayload {
    public BpmnProjection {
        commands = List.copyOf(commands == null ? List.of() : commands);
        events = List.copyOf(events == null ? List.of() : events);
        processRelationshipRefs = List.copyOf(processRelationshipRefs == null ? List.of() : processRelationshipRefs);
    }
}
