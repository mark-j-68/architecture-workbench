package com.architectureworkbench.core.model.governance;

import java.util.Objects;

/**
 * Cross-cutting governance requirements for regulated architecture workspaces.
 */
public class GovernanceSection {
    private AiGovernanceSection ai = new AiGovernanceSection();
    private AuditGovernanceSection audit = new AuditGovernanceSection();
    private PiiProtectionPolicy piiProtection = new PiiProtectionPolicy();

    public AiGovernanceSection getAi() { return ai; }
    public void setAi(AiGovernanceSection ai) { this.ai = Objects.requireNonNullElseGet(ai, AiGovernanceSection::new); }
    public AuditGovernanceSection getAudit() { return audit; }
    public void setAudit(AuditGovernanceSection audit) { this.audit = Objects.requireNonNullElseGet(audit, AuditGovernanceSection::new); }
    public PiiProtectionPolicy getPiiProtection() { return piiProtection; }
    public void setPiiProtection(PiiProtectionPolicy piiProtection) { this.piiProtection = Objects.requireNonNullElseGet(piiProtection, PiiProtectionPolicy::new); }
}
