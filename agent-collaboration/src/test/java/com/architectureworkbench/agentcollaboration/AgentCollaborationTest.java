package com.architectureworkbench.agentcollaboration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCollaborationTest {
    @Test
    void singleReviewerResponseEmitsImmutableAuditEvent() {
        ImmutableAgentAuditLog auditLog = new ImmutableAgentAuditLog();
        ArchitectureReviewer reviewer = new DddReviewerStub(auditLog);
        ReviewRequest request = request("Commands exist without ownership evidence.");

        ReviewResponse response = reviewer.review(request);

        assertEquals(ReviewerType.DDD, response.reviewerType());
        assertEquals(1, response.findings().size());
        assertEquals("ddd-command-without-aggregate", response.findings().get(0).findingKey());
        assertFalse(response.auditEventId().isBlank());
        assertEquals(1, auditLog.entriesForReview(request.reviewId()).size());
        assertEquals("false", auditLog.entries().get(0).details().get("externalProviderCalled"));
    }

    @Test
    void multipleReviewersAgreeingProduceAgreedFindings() {
        ImmutableAgentAuditLog auditLog = new ImmutableAgentAuditLog();
        ReviewRequest request = request("PII exists without encryption evidence.");
        ConsensusService consensusService = new ConsensusService(auditLog);
        List<ReviewResponse> responses = List.of(
                response(request, ReviewerType.SECURITY, finding("pii-control-gap", ReviewerType.SECURITY, FindingSeverity.CRITICAL, "Add encrypted payload handling.")),
                response(request, ReviewerType.REGULATORY, finding("pii-control-gap", ReviewerType.REGULATORY, FindingSeverity.CRITICAL, "Add encrypted payload handling."))
        );

        ReviewConsensus consensus = consensusService.generateConsensus(request, responses);

        assertEquals(1, consensus.agreedFindings().size());
        assertTrue(consensus.conflictingFindings().isEmpty());
        assertTrue(consensus.confidenceScore() > 0.7);
        assertEquals("Create remediation decisions for agreed high-severity findings before implementation.", consensus.recommendedNextAction());
        assertEquals(1, auditLog.entriesForReview(request.reviewId()).size());
    }

    @Test
    void reviewersDisagreeingProduceConflictingFindings() {
        ImmutableAgentAuditLog auditLog = new ImmutableAgentAuditLog();
        ReviewRequest request = request("API and PII controls are under review.");
        ConsensusService consensusService = new ConsensusService(auditLog);
        List<ReviewResponse> responses = List.of(
                response(request, ReviewerType.SECURITY, finding("api-auth-gap", ReviewerType.SECURITY, FindingSeverity.ERROR, "Add authentication evidence.")),
                response(request, ReviewerType.REGULATORY, finding("api-auth-gap", ReviewerType.REGULATORY, FindingSeverity.WARNING, "Record compensating control evidence."))
        );

        ReviewConsensus consensus = consensusService.generateConsensus(request, responses);

        assertTrue(consensus.agreedFindings().isEmpty());
        assertEquals(2, consensus.conflictingFindings().size());
        assertEquals("Escalate to human architecture review because reviewer findings conflict.", consensus.recommendedNextAction());
    }

    @Test
    void consensusGenerationRunsReviewersAndAuditsWholeReviewRun() {
        ImmutableAgentAuditLog auditLog = new ImmutableAgentAuditLog();
        ReviewRequest request = request("PII is processed without encryption evidence.");
        ReviewRunService reviewRunService = new ReviewRunService(
                List.of(new FixedReviewer(ReviewerType.SECURITY, "shared-control-gap"), new FixedReviewer(ReviewerType.REGULATORY, "shared-control-gap")),
                new ConsensusService(auditLog),
                auditLog
        );

        ReviewRunResult result = reviewRunService.runReview(request);

        assertEquals(2, result.responses().size());
        assertFalse(result.consensus().auditEventId().isBlank());
        assertEquals(1, result.consensus().agreedFindings().size());
        assertTrue(result.consensus().conflictingFindings().isEmpty());
        assertEquals(3, auditLog.entriesForReview(request.reviewId()).size());
        assertEquals("ReviewRequested", auditLog.entries().get(0).action());
        assertEquals("ReviewCompleted", auditLog.entries().get(2).action());
        assertEquals("workspace-test", auditLog.entries().get(0).architectureEvent().workspaceId());
        assertFalse(auditLog.entries().get(0).architectureEvent().causationId().value().isBlank());
        assertFalse(auditLog.entries().get(0).architectureEvent().correlationId().value().isBlank());
        assertEquals("REQUIRED", auditLog.entries().get(0).details().get("auditRelevance"));
        assertEquals("NEITHER", auditLog.entries().get(0).details().get("mutationTarget"));
        assertFalse(auditLog.entries().get(1).previousHash().equals("GENESIS"));
    }

    private static ReviewRequest request(String context) {
        return new ReviewRequest("review-test", "workspace-test", "architect", context, List.of(), Map.of());
    }

    private static ReviewFinding finding(String key, ReviewerType type, FindingSeverity severity, String recommendation) {
        return new ReviewFinding(
                key,
                type,
                severity,
                "Shared finding",
                "Finding description",
                recommendation,
                0.82,
                Map.of()
        );
    }

    private static ReviewResponse response(ReviewRequest request, ReviewerType type, ReviewFinding finding) {
        return new ReviewResponse(
                request.reviewId(),
                type,
                type.name().toLowerCase() + "-test-reviewer",
                "Test response",
                List.of(finding),
                finding.confidence(),
                Instant.now(),
                ""
        );
    }

    private static class FixedReviewer implements ArchitectureReviewer {
        private final ReviewerType type;
        private final String findingKey;

        private FixedReviewer(ReviewerType type, String findingKey) {
            this.type = type;
            this.findingKey = findingKey;
        }

        @Override public ReviewerType reviewerType() { return type; }
        @Override public String reviewerId() { return type.name().toLowerCase() + "-fixed-reviewer"; }

        @Override
        public ReviewResponse review(ReviewRequest request) {
            return response(request, type, finding(findingKey, type, FindingSeverity.ERROR, "Create a remediation decision."));
        }
    }
}
