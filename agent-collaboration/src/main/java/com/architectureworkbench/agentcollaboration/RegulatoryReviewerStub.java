package com.architectureworkbench.agentcollaboration;

import java.util.ArrayList;
import java.util.List;

public class RegulatoryReviewerStub extends StubArchitectureReviewer {
    public RegulatoryReviewerStub(ImmutableAgentAuditLog auditLog) {
        super(auditLog);
    }

    @Override public ReviewerType reviewerType() { return ReviewerType.REGULATORY; }
    @Override public String reviewerId() { return "regulatory-reviewer-stub"; }

    @Override
    protected List<ReviewFinding> generateFindings(ReviewRequest request) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (mentions(request, "pii") && !mentions(request, "shredd")) {
            findings.add(finding(
                    "regulatory-crypto-shredding-missing",
                    FindingSeverity.CRITICAL,
                    "Cryptographic shredding evidence is missing",
                    "PII is referenced but cryptographic shredding support is not evidenced.",
                    "Add key-destruction workflow evidence and protected payload references.",
                    0.88
            ));
        }
        if (!mentions(request, "audit")) {
            findings.add(finding(
                    "regulatory-audit-trace-missing",
                    FindingSeverity.ERROR,
                    "Immutable audit trace evidence is missing",
                    "The review context does not explicitly show immutable audit trace coverage.",
                    "Record prompt, response, tool, evidence, and decision outcome traceability.",
                    0.8
            ));
        }
        return findings;
    }
}
