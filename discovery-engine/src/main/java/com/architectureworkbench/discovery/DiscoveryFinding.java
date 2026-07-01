package com.architectureworkbench.discovery;

import java.util.Objects;
import java.util.UUID;

public record DiscoveryFinding(
        String findingId,
        DiscoveryFindingSeverity severity,
        String ruleId,
        String message,
        String artifactRef
) {
    public DiscoveryFinding {
        findingId = Objects.requireNonNullElseGet(findingId, () -> "discovery-finding-" + UUID.randomUUID());
        severity = Objects.requireNonNull(severity, "severity");
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("Discovery finding rule id is required.");
        }
        message = Objects.requireNonNullElse(message, "");
        artifactRef = Objects.requireNonNullElse(artifactRef, "");
    }
}
