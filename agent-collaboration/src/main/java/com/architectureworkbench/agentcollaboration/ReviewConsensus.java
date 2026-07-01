package com.architectureworkbench.agentcollaboration;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ReviewConsensus(
        String reviewId,
        List<ReviewFinding> agreedFindings,
        List<ReviewFinding> conflictingFindings,
        double confidenceScore,
        String recommendedNextAction,
        Instant generatedAt,
        String auditEventId
) {
    public ReviewConsensus {
        reviewId = ReviewRequest.requireText(reviewId, "reviewId");
        agreedFindings = List.copyOf(Objects.requireNonNullElseGet(agreedFindings, List::of));
        conflictingFindings = List.copyOf(Objects.requireNonNullElseGet(conflictingFindings, List::of));
        if (confidenceScore < 0.0 || confidenceScore > 1.0) {
            throw new IllegalArgumentException("confidenceScore must be between 0.0 and 1.0.");
        }
        recommendedNextAction = Objects.requireNonNullElse(recommendedNextAction, "");
        generatedAt = Objects.requireNonNullElseGet(generatedAt, Instant::now);
        auditEventId = Objects.requireNonNullElse(auditEventId, "");
    }
}
