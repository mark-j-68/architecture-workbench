package com.architectureworkbench.agentcollaboration;

import java.util.Map;
import java.util.Objects;

public record ReviewFinding(
        String findingKey,
        ReviewerType reviewerType,
        FindingSeverity severity,
        String title,
        String description,
        String recommendation,
        double confidence,
        Map<String, String> evidenceRefs
) {
    public ReviewFinding {
        findingKey = ReviewRequest.requireText(findingKey, "findingKey");
        reviewerType = Objects.requireNonNull(reviewerType, "reviewerType");
        severity = Objects.requireNonNull(severity, "severity");
        title = ReviewRequest.requireText(title, "title");
        description = Objects.requireNonNullElse(description, "");
        recommendation = Objects.requireNonNullElse(recommendation, "");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0.");
        }
        evidenceRefs = Map.copyOf(Objects.requireNonNullElseGet(evidenceRefs, Map::of));
    }

    public boolean sameClaimAs(ReviewFinding other) {
        return findingKey.equals(other.findingKey());
    }
}
