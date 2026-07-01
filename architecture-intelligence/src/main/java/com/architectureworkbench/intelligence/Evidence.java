package com.architectureworkbench.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Evidence(
        String id,
        String source,
        String provenance,
        double confidence,
        Instant timestamp,
        List<String> references,
        List<String> supportingArtifacts
) {
    public Evidence {
        id = AimIds.id("evidence", id);
        source = AimIds.required(source, "source");
        provenance = Objects.requireNonNullElse(provenance, "");
        confidence = AimIds.confidence(confidence);
        timestamp = Objects.requireNonNullElseGet(timestamp, Instant::now);
        references = List.copyOf(references == null ? List.of() : references);
        supportingArtifacts = List.copyOf(supportingArtifacts == null ? List.of() : supportingArtifacts);
    }
}
