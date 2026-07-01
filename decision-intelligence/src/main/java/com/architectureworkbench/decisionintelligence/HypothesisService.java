package com.architectureworkbench.decisionintelligence;

import com.architectureworkbench.intelligence.ConfidenceCalculator;
import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Recommendation;
import java.util.List;

public class HypothesisService {
    private final ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator();

    public Hypothesis createHypothesis(
            String statement,
            String rationale,
            List<Evidence> supportingEvidence,
            List<Recommendation> relatedRecommendations
    ) {
        double evidenceConfidence = confidenceCalculator.calculateEvidenceConfidence(supportingEvidence);
        double recommendationConfidence = confidenceCalculator.calculateRecommendationConfidence(
                relatedRecommendations.stream().flatMap(recommendation -> recommendation.supportingFindings().stream()).toList(),
                relatedRecommendations.stream().flatMap(recommendation -> recommendation.relatedConcerns().stream()).toList()
        );
        return new Hypothesis(
                null,
                statement,
                rationale,
                supportingEvidence,
                relatedRecommendations,
                round((evidenceConfidence + recommendationConfidence) / 2.0),
                HypothesisStatus.PROPOSED
        );
    }

    public Hypothesis validateHypothesis(Hypothesis hypothesis) {
        return new Hypothesis(
                hypothesis.id(),
                hypothesis.statement(),
                hypothesis.rationale(),
                hypothesis.supportingEvidence(),
                hypothesis.relatedRecommendations(),
                hypothesis.confidence(),
                hypothesis.confidence() >= 0.5 ? HypothesisStatus.VALIDATED : HypothesisStatus.INVALIDATED
        );
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
