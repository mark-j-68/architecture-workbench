package com.architectureworkbench.agentcollaboration;

import java.util.ArrayList;
import java.util.List;

public class SecurityReviewerStub extends StubArchitectureReviewer {
    public SecurityReviewerStub(ImmutableAgentAuditLog auditLog) {
        super(auditLog);
    }

    @Override public ReviewerType reviewerType() { return ReviewerType.SECURITY; }
    @Override public String reviewerId() { return "security-reviewer-stub"; }

    @Override
    protected List<ReviewFinding> generateFindings(ReviewRequest request) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (mentions(request, "pii") && !mentions(request, "encrypt")) {
            findings.add(finding(
                    "security-pii-encryption-missing",
                    FindingSeverity.CRITICAL,
                    "PII encryption evidence is missing",
                    "PII is referenced but encryption controls are not documented in the review context.",
                    "Add encrypted payload handling and key-management evidence before approval.",
                    0.9
            ));
        }
        if (mentions(request, "api") && !mentions(request, "auth")) {
            findings.add(finding(
                    "security-api-auth-missing",
                    FindingSeverity.ERROR,
                    "API authentication evidence is missing",
                    "API exposure is described without authentication or authorization evidence.",
                    "Record authentication, authorization, and threat model evidence for exposed APIs.",
                    0.82
            ));
        }
        return findings;
    }
}
