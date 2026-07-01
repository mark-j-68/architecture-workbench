package com.architectureworkbench.decisionintelligence;

import java.util.List;

public record Learning(
        String id,
        String summary,
        List<Experiment> relatedExperiments,
        double confidence,
        List<Pattern> reusablePatterns
) {
    public Learning {
        id = DecisionIntelligenceIds.id("learning", id);
        summary = DecisionIntelligenceIds.required(summary, "summary");
        relatedExperiments = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(relatedExperiments == null ? List.of() : relatedExperiments, "relatedExperiments"));
        confidence = DecisionIntelligenceIds.confidence(confidence);
        reusablePatterns = List.copyOf(reusablePatterns == null ? List.of() : reusablePatterns);
    }

    public List<Pattern> produceReusableArchitecturalPatterns() {
        return reusablePatterns;
    }
}
