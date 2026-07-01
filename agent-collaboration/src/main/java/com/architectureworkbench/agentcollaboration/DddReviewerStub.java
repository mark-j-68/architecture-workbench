package com.architectureworkbench.agentcollaboration;

import java.util.ArrayList;
import java.util.List;

public class DddReviewerStub extends StubArchitectureReviewer {
    public DddReviewerStub(ImmutableAgentAuditLog auditLog) {
        super(auditLog);
    }

    @Override public ReviewerType reviewerType() { return ReviewerType.DDD; }
    @Override public String reviewerId() { return "ddd-reviewer-stub"; }

    @Override
    protected List<ReviewFinding> generateFindings(ReviewRequest request) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (mentions(request, "command") && !mentions(request, "aggregate")) {
            findings.add(finding(
                    "ddd-command-without-aggregate",
                    FindingSeverity.ERROR,
                    "Command lacks aggregate ownership",
                    "The review context references commands but does not identify aggregate handlers.",
                    "Link each command to the aggregate that handles it.",
                    0.84
            ));
        }
        if (mentions(request, "aggregate") && !mentions(request, "root")) {
            findings.add(finding(
                    "ddd-aggregate-root-missing",
                    FindingSeverity.WARNING,
                    "Aggregate root evidence is incomplete",
                    "Aggregates are present but root entity evidence is not explicit.",
                    "Record root entities and invariants for each aggregate.",
                    0.76
            ));
        }
        return findings;
    }
}
