package com.architectureworkbench.decisionintelligence;

import com.architectureworkbench.intelligence.Metric;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Experiment(
        String id,
        Hypothesis hypothesis,
        Instant implementationDate,
        List<String> expectedOutcomes,
        List<String> actualOutcomes,
        List<Metric> metrics
) {
    public Experiment {
        id = DecisionIntelligenceIds.id("experiment", id);
        hypothesis = Objects.requireNonNull(hypothesis, "hypothesis");
        implementationDate = Objects.requireNonNullElseGet(implementationDate, Instant::now);
        expectedOutcomes = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(expectedOutcomes == null ? List.of() : expectedOutcomes, "expectedOutcomes"));
        actualOutcomes = List.copyOf(actualOutcomes == null ? List.of() : actualOutcomes);
        metrics = List.copyOf(metrics == null ? List.of() : metrics);
    }
}
