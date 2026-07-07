package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DiscoveryEvidence(
        String evidenceId,
        String evidenceType,
        String source,
        String provenance,
        String identity,
        DiscoveryConfidence confidence,
        boolean directlyObserved,
        Instant discoveredAt,
        List<String> references,
        Map<String, String> attributes
) {
    public DiscoveryEvidence {
        evidenceId = Objects.requireNonNullElseGet(evidenceId, () -> "discovery-evidence-" + UUID.randomUUID());
        evidenceType = required(evidenceType, "evidenceType");
        source = Objects.requireNonNullElse(source, "");
        provenance = Objects.requireNonNullElse(provenance, "");
        identity = Objects.requireNonNullElse(identity, "");
        confidence = Objects.requireNonNull(confidence, "confidence");
        discoveredAt = Objects.requireNonNullElseGet(discoveredAt, Instant::now);
        references = List.copyOf(references == null ? List.of() : references);
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
