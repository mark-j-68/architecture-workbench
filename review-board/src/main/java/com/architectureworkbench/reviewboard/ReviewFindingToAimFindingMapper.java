package com.architectureworkbench.reviewboard;

import com.architectureworkbench.agentcollaboration.FindingSeverity;
import com.architectureworkbench.agentcollaboration.ReviewFinding;
import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Severity;
import java.time.Instant;
import java.util.List;

public class ReviewFindingToAimFindingMapper {
    public Finding map(String reviewId, ReviewFinding reviewFinding) {
        Evidence evidence = new Evidence(
                "evidence-review-" + reviewId + "-" + reviewFinding.findingKey(),
                "review-board:" + reviewFinding.reviewerType().name(),
                "Provider-neutral review finding " + reviewFinding.findingKey(),
                reviewFinding.confidence(),
                Instant.now(),
                reviewFinding.evidenceRefs().values().stream().toList(),
                List.of(reviewId, reviewFinding.findingKey())
        );
        Observation observation = new Observation(
                "observation-review-" + reviewId + "-" + reviewFinding.findingKey(),
                "review-board",
                reviewFinding.description(),
                List.of(evidence),
                List.of()
        );
        return new Finding(
                "finding-review-" + reviewId + "-" + reviewFinding.findingKey(),
                severity(reviewFinding.severity()),
                "REVIEW:" + reviewFinding.reviewerType().name(),
                reviewFinding.title(),
                List.of(observation),
                reviewFinding.confidence()
        );
    }

    private static Severity severity(FindingSeverity severity) {
        return switch (severity) {
            case INFO -> Severity.INFO;
            case WARNING -> Severity.WARNING;
            case ERROR -> Severity.ERROR;
            case CRITICAL -> Severity.CRITICAL;
        };
    }
}
