package com.architectureworkbench.agentcollaboration;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ReviewResponse(
        String reviewId,
        ReviewerType reviewerType,
        String reviewerId,
        String summary,
        List<ReviewFinding> findings,
        double confidence,
        Instant reviewedAt,
        String auditEventId
) {
    public ReviewResponse {
        reviewId = ReviewRequest.requireText(reviewId, "reviewId");
        reviewerType = Objects.requireNonNull(reviewerType, "reviewerType");
        reviewerId = ReviewRequest.requireText(reviewerId, "reviewerId");
        summary = Objects.requireNonNullElse(summary, "");
        findings = List.copyOf(Objects.requireNonNullElseGet(findings, List::of));
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0.");
        }
        reviewedAt = Objects.requireNonNullElseGet(reviewedAt, Instant::now);
        auditEventId = Objects.requireNonNullElse(auditEventId, "");
    }
}
