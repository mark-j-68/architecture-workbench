package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DiscoveryObservation(
        String observationId,
        String observationType,
        String description,
        DiscoveryConfidence confidence,
        List<String> relatedEvidenceIds,
        Instant observedAt
) {
    public DiscoveryObservation {
        observationId = Objects.requireNonNullElseGet(observationId, () -> "discovery-observation-" + UUID.randomUUID());
        observationType = required(observationType, "observationType");
        description = required(description, "description");
        confidence = Objects.requireNonNull(confidence, "confidence");
        relatedEvidenceIds = List.copyOf(relatedEvidenceIds == null ? List.of() : relatedEvidenceIds);
        observedAt = Objects.requireNonNullElseGet(observedAt, Instant::now);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
