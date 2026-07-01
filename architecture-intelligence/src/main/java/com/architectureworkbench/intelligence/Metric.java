package com.architectureworkbench.intelligence;

import java.util.List;

public record Metric(
        String id,
        String name,
        double value,
        Trend trend,
        List<Evidence> evidence
) {
    public Metric {
        id = AimIds.id("metric", id);
        name = AimIds.required(name, "name");
        evidence = List.copyOf(AimIds.requireNonEmpty(evidence == null ? List.of() : evidence, "evidence"));
    }
}
