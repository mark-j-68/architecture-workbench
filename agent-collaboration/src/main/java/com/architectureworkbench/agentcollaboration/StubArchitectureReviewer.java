package com.architectureworkbench.agentcollaboration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

abstract class StubArchitectureReviewer implements ArchitectureReviewer {
    private final ImmutableAgentAuditLog auditLog;

    protected StubArchitectureReviewer(ImmutableAgentAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public final ReviewResponse review(ReviewRequest request) {
        List<ReviewFinding> findings = generateFindings(request);
        double confidence = findings.isEmpty()
                ? 0.72
                : findings.stream().mapToDouble(ReviewFinding::confidence).average().orElse(0.72);
        AgentAuditEvent auditEvent = auditLog.append(
                request.workspaceId(),
                request.actorRef(),
                "ARCHITECTURE_REVIEWER_STUB_RUN",
                request.reviewId(),
                Map.of(
                        "reviewerType", reviewerType().name(),
                        "reviewerId", reviewerId(),
                        "findingCount", String.valueOf(findings.size()),
                        "externalProviderCalled", "false"
                )
        );
        return new ReviewResponse(
                request.reviewId(),
                reviewerType(),
                reviewerId(),
                "%s stub produced %d finding(s).".formatted(reviewerType(), findings.size()),
                findings,
                confidence,
                Instant.now(),
                auditEvent.eventId()
        );
    }

    protected abstract List<ReviewFinding> generateFindings(ReviewRequest request);

    protected ReviewFinding finding(String key, FindingSeverity severity, String title, String description, String recommendation, double confidence) {
        return new ReviewFinding(key, reviewerType(), severity, title, description, recommendation, confidence, Map.of());
    }

    protected boolean mentions(ReviewRequest request, String token) {
        return request.architectureContext().toLowerCase().contains(token.toLowerCase())
                || request.focusAreas().stream().anyMatch(area -> area.toLowerCase().contains(token.toLowerCase()));
    }
}
