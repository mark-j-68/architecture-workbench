package com.architectureworkbench.agentcollaboration;

import java.util.ArrayList;
import java.util.List;

public class CloudReviewerStub extends StubArchitectureReviewer {
    public CloudReviewerStub(ImmutableAgentAuditLog auditLog) {
        super(auditLog);
    }

    @Override public ReviewerType reviewerType() { return ReviewerType.CLOUD; }
    @Override public String reviewerId() { return "cloud-reviewer-stub"; }

    @Override
    protected List<ReviewFinding> generateFindings(ReviewRequest request) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (mentions(request, "queue") && !mentions(request, "dead-letter")) {
            findings.add(finding(
                    "cloud-queue-dlq-missing",
                    FindingSeverity.WARNING,
                    "Queue resilience evidence is missing",
                    "Queue usage is described without dead-letter or retry handling evidence.",
                    "Add retry, dead-letter, and operational alarm evidence for asynchronous integrations.",
                    0.74
            ));
        }
        if (mentions(request, "s3") && !mentions(request, "retention")) {
            findings.add(finding(
                    "cloud-storage-retention-missing",
                    FindingSeverity.WARNING,
                    "Storage retention policy is unclear",
                    "Object storage is referenced without lifecycle or retention evidence.",
                    "Record retention, lifecycle, and deletion controls for object storage.",
                    0.71
            ));
        }
        return findings;
    }
}
