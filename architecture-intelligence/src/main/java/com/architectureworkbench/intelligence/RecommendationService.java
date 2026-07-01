package com.architectureworkbench.intelligence;

import java.util.ArrayList;
import java.util.List;

public class RecommendationService {
    private final ConfidenceCalculator confidenceCalculator;

    public RecommendationService(ConfidenceCalculator confidenceCalculator) {
        this.confidenceCalculator = confidenceCalculator;
    }

    public Recommendation promoteFindingsIntoRecommendation(
            String description,
            String rationale,
            List<Finding> findings,
            String estimatedImpact,
            String estimatedEffort
    ) {
        double confidence = confidenceCalculator.calculateRecommendationConfidence(findings, List.of());
        return new Recommendation(
                null,
                description,
                rationale,
                List.of(),
                findings,
                estimatedImpact,
                estimatedEffort,
                confidence,
                LifecycleStatus.PROPOSED
        );
    }

    public Recommendation associateWithConcerns(Recommendation recommendation, List<Concern> concerns) {
        List<Concern> allConcerns = new ArrayList<>(recommendation.relatedConcerns());
        allConcerns.addAll(concerns);
        double confidence = confidenceCalculator.calculateRecommendationConfidence(recommendation.supportingFindings(), allConcerns);
        return new Recommendation(
                recommendation.id(),
                recommendation.description(),
                recommendation.rationale(),
                allConcerns,
                recommendation.supportingFindings(),
                recommendation.estimatedImpact(),
                recommendation.estimatedEffort(),
                confidence,
                recommendation.lifecycleStatus()
        );
    }
}
