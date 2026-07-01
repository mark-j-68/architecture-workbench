package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record OpenApiProjection(
        String title,
        List<String> capabilities,
        List<String> commandsAsOperations,
        List<String> evidenceRefs
) implements ProjectionPayload {
    public OpenApiProjection {
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        commandsAsOperations = List.copyOf(commandsAsOperations == null ? List.of() : commandsAsOperations);
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
    }
}
