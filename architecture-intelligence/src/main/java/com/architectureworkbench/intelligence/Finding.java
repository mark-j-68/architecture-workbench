package com.architectureworkbench.intelligence;

import java.util.List;

public record Finding(
        String id,
        Severity severity,
        String category,
        String description,
        List<Observation> supportingObservations,
        double confidence
) {
    public Finding {
        id = AimIds.id("finding", id);
        severity = java.util.Objects.requireNonNull(severity, "severity");
        category = AimIds.required(category, "category");
        description = AimIds.required(description, "description");
        supportingObservations = List.copyOf(AimIds.requireNonEmpty(supportingObservations == null ? List.of() : supportingObservations, "supportingObservations"));
        confidence = AimIds.confidence(confidence);
    }
}
