package com.architectureworkbench.intelligence;

import java.util.List;

public record Observation(
        String id,
        String source,
        String description,
        List<Evidence> relatedEvidence,
        List<String> relatedGraphElements
) {
    public Observation {
        id = AimIds.id("observation", id);
        source = AimIds.required(source, "source");
        description = AimIds.required(description, "description");
        relatedEvidence = List.copyOf(AimIds.requireNonEmpty(relatedEvidence == null ? List.of() : relatedEvidence, "relatedEvidence"));
        relatedGraphElements = List.copyOf(relatedGraphElements == null ? List.of() : relatedGraphElements);
    }
}
