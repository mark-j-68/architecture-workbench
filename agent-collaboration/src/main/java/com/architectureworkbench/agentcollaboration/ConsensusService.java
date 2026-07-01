package com.architectureworkbench.agentcollaboration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConsensusService {
    private final ImmutableAgentAuditLog auditLog;

    public ConsensusService(ImmutableAgentAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public ReviewConsensus generateConsensus(ReviewRequest request, List<ReviewResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            throw new IllegalArgumentException("At least one review response is required.");
        }
        responses.forEach(response -> {
            if (!Objects.equals(response.reviewId(), request.reviewId())) {
                throw new IllegalArgumentException("All responses must belong to request reviewId " + request.reviewId());
            }
        });

        Map<String, List<ReviewFinding>> findingsByKey = new LinkedHashMap<>();
        responses.stream()
                .flatMap(response -> response.findings().stream())
                .forEach(finding -> findingsByKey.computeIfAbsent(finding.findingKey(), ignored -> new ArrayList<>()).add(finding));

        int reviewerCount = responses.size();
        List<ReviewFinding> agreed = new ArrayList<>();
        List<ReviewFinding> conflicting = new ArrayList<>();

        findingsByKey.values().forEach(group -> {
            if (group.size() > 1 && sameSeverity(group) && compatibleRecommendations(group)) {
                agreed.add(mergeAgreedFinding(group));
            } else {
                conflicting.addAll(group);
            }
        });

        double coverage = findingsByKey.isEmpty() ? 1.0 : (double) agreed.size() / findingsByKey.size();
        double reviewerConfidence = responses.stream().mapToDouble(ReviewResponse::confidence).average().orElse(0.0);
        double confidenceScore = round((coverage * 0.6) + (reviewerConfidence * 0.4));
        String nextAction = recommendedNextAction(agreed, conflicting, reviewerCount);

        AgentAuditEvent auditEvent = auditLog.append(
                request.workspaceId(),
                request.actorRef(),
                "ARCHITECTURE_REVIEW_CONSENSUS_GENERATED",
                request.reviewId(),
                Map.of(
                        "reviewerCount", String.valueOf(reviewerCount),
                        "agreedFindingCount", String.valueOf(agreed.size()),
                        "conflictingFindingCount", String.valueOf(conflicting.size()),
                        "externalProviderCalled", "false"
                )
        );

        return new ReviewConsensus(
                request.reviewId(),
                agreed,
                conflicting,
                confidenceScore,
                nextAction,
                Instant.now(),
                auditEvent.eventId()
        );
    }

    private static boolean sameSeverity(List<ReviewFinding> findings) {
        FindingSeverity severity = findings.get(0).severity();
        return findings.stream().allMatch(finding -> finding.severity() == severity);
    }

    private static boolean compatibleRecommendations(List<ReviewFinding> findings) {
        String first = normalize(findings.get(0).recommendation());
        return findings.stream().allMatch(finding -> normalize(finding.recommendation()).equals(first));
    }

    private static ReviewFinding mergeAgreedFinding(List<ReviewFinding> findings) {
        ReviewFinding primary = findings.stream()
                .max(Comparator.comparingDouble(ReviewFinding::confidence))
                .orElseThrow();
        double confidence = round(findings.stream().mapToDouble(ReviewFinding::confidence).average().orElse(primary.confidence()));
        return new ReviewFinding(
                primary.findingKey(),
                primary.reviewerType(),
                primary.severity(),
                primary.title(),
                primary.description(),
                primary.recommendation(),
                confidence,
                primary.evidenceRefs()
        );
    }

    private static String recommendedNextAction(List<ReviewFinding> agreed, List<ReviewFinding> conflicting, int reviewerCount) {
        if (!conflicting.isEmpty()) {
            return "Escalate to human architecture review because reviewer findings conflict.";
        }
        if (agreed.stream().anyMatch(finding -> finding.severity() == FindingSeverity.CRITICAL || finding.severity() == FindingSeverity.ERROR)) {
            return "Create remediation decisions for agreed high-severity findings before implementation.";
        }
        if (agreed.isEmpty() && reviewerCount > 1) {
            return "Proceed with human sign-off; no reviewer findings were raised.";
        }
        return "Proceed with recorded recommendations and continue normal governance.";
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
