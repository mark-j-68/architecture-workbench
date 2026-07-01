package com.architectureworkbench.decisionintelligence;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Recommendation;
import java.util.List;
import java.util.Objects;

public record Hypothesis(
        String id,
        String statement,
        String rationale,
        List<Evidence> supportingEvidence,
        List<Recommendation> relatedRecommendations,
        double confidence,
        HypothesisStatus status
) {
    public Hypothesis {
        id = DecisionIntelligenceIds.id("hypothesis", id);
        statement = DecisionIntelligenceIds.required(statement, "statement");
        rationale = DecisionIntelligenceIds.required(rationale, "rationale");
        supportingEvidence = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(supportingEvidence == null ? List.of() : supportingEvidence, "supportingEvidence"));
        relatedRecommendations = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(relatedRecommendations == null ? List.of() : relatedRecommendations, "relatedRecommendations"));
        confidence = DecisionIntelligenceIds.confidence(confidence);
        status = Objects.requireNonNullElse(status, HypothesisStatus.PROPOSED);
    }
}
