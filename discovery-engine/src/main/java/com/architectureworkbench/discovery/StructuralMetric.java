package com.architectureworkbench.discovery;

import java.util.List;

public record StructuralMetric(String name, double value, String unit, List<String> evidenceIds, String derivation) {
    public StructuralMetric {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required.");
        unit = unit == null ? "count" : unit;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        derivation = derivation == null ? "" : derivation;
    }
}
