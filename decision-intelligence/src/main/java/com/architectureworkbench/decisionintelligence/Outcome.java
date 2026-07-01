package com.architectureworkbench.decisionintelligence;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Observation;
import java.util.List;
import java.util.Objects;

public record Outcome(
        String id,
        SuccessLevel successLevel,
        List<Observation> observations,
        List<Evidence> supportingEvidence
) {
    public Outcome {
        id = DecisionIntelligenceIds.id("outcome", id);
        successLevel = Objects.requireNonNull(successLevel, "successLevel");
        observations = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(observations == null ? List.of() : observations, "observations"));
        supportingEvidence = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(supportingEvidence == null ? List.of() : supportingEvidence, "supportingEvidence"));
    }
}
