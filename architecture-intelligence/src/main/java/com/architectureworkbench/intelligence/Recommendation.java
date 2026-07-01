package com.architectureworkbench.intelligence;

import java.util.List;

public record Recommendation(
        String id,
        String description,
        String rationale,
        List<Concern> relatedConcerns,
        List<Finding> supportingFindings,
        String estimatedImpact,
        String estimatedEffort,
        double confidence,
        LifecycleStatus lifecycleStatus
) {
    public Recommendation {
        id = AimIds.id("recommendation", id);
        description = AimIds.required(description, "description");
        rationale = AimIds.required(rationale, "rationale");
        relatedConcerns = List.copyOf(relatedConcerns == null ? List.of() : relatedConcerns);
        supportingFindings = List.copyOf(AimIds.requireNonEmpty(supportingFindings == null ? List.of() : supportingFindings, "supportingFindings"));
        estimatedImpact = java.util.Objects.requireNonNullElse(estimatedImpact, "");
        estimatedEffort = java.util.Objects.requireNonNullElse(estimatedEffort, "");
        confidence = AimIds.confidence(confidence);
        lifecycleStatus = java.util.Objects.requireNonNullElse(lifecycleStatus, LifecycleStatus.PROPOSED);
    }
}
