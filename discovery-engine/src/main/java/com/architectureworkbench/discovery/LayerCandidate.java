package com.architectureworkbench.discovery;

import java.util.List;
import java.util.Objects;

public record LayerCandidate(String name, String symbol, DiscoveryConfidence confidence, List<String> evidenceIds) {
    public LayerCandidate {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required.");
        symbol = Objects.requireNonNullElse(symbol, "");
        confidence = Objects.requireNonNull(confidence, "confidence");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }
}
