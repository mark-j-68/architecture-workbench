package com.architectureworkbench.agentcollaboration;

import java.util.ArrayList;
import java.util.List;

public class DeliveryReviewerStub extends StubArchitectureReviewer {
    public DeliveryReviewerStub(ImmutableAgentAuditLog auditLog) {
        super(auditLog);
    }

    @Override public ReviewerType reviewerType() { return ReviewerType.DELIVERY; }
    @Override public String reviewerId() { return "delivery-reviewer-stub"; }

    @Override
    protected List<ReviewFinding> generateFindings(ReviewRequest request) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (!mentions(request, "adr")) {
            findings.add(finding(
                    "delivery-adr-missing",
                    FindingSeverity.WARNING,
                    "ADR evidence is missing",
                    "The review context does not identify an ADR for the material decision.",
                    "Draft or link an ADR before implementation handoff.",
                    0.72
            ));
        }
        if (mentions(request, "implementation") && !mentions(request, "test")) {
            findings.add(finding(
                    "delivery-test-strategy-missing",
                    FindingSeverity.WARNING,
                    "Test strategy is unclear",
                    "Implementation is referenced without a delivery test strategy.",
                    "Add build, test, and acceptance criteria before agent handoff.",
                    0.69
            ));
        }
        return findings;
    }
}
