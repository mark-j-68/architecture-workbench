package com.architectureworkbench.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DecisionOutcome(
        String id,
        DecisionStatus status,
        String rationale,
        List<Reviewer> reviewers,
        List<Evidence> evidence,
        List<Recommendation> recommendations,
        Instant timestamp
) {
    public DecisionOutcome {
        id = AimIds.id("decision-outcome", id);
        status = Objects.requireNonNull(status, "status");
        rationale = AimIds.required(rationale, "rationale");
        reviewers = List.copyOf(reviewers == null ? List.of() : reviewers);
        evidence = List.copyOf(AimIds.requireNonEmpty(evidence == null ? List.of() : evidence, "evidence"));
        recommendations = List.copyOf(AimIds.requireNonEmpty(recommendations == null ? List.of() : recommendations, "recommendations"));
        timestamp = Objects.requireNonNullElseGet(timestamp, Instant::now);
    }
}
