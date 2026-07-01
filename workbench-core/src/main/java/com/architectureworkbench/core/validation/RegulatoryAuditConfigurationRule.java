package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.governance.AuditGovernanceSection;
import com.architectureworkbench.core.model.governance.PiiProtectionPolicy;
import com.architectureworkbench.core.model.validation.Severity;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.ArrayList;
import java.util.List;

public class RegulatoryAuditConfigurationRule implements ValidationRule {
    @Override public String id() { return "GOV-AUDIT-001"; }
    @Override public String description() { return "Regulatory mode requires immutable logging with PII separation and crypto-shredding."; }

    @Override
    public List<ValidationFinding> validate(ArchitectureModel model) {
        List<ValidationFinding> findings = new ArrayList<>();
        AuditGovernanceSection audit = model.getGovernance().getAudit();
        PiiProtectionPolicy pii = model.getGovernance().getPiiProtection();
        if (!audit.isImmutableActivityLogRequired()) {
            findings.add(new ValidationFinding(id(), Severity.ERROR,
                    "Immutable activity logging must be enabled for regulated workspaces.",
                    "governance.audit.immutableActivityLogRequired"));
        }
        if (audit.isPiiAllowedInImmutableEnvelope()) {
            findings.add(new ValidationFinding(id(), Severity.ERROR,
                    "PII must not be written into the immutable audit envelope; use encrypted payload references.",
                    "governance.audit.piiAllowedInImmutableEnvelope"));
        }
        if (!pii.isEncryptionRequired() || !pii.isCryptographicShreddingRequired()) {
            findings.add(new ValidationFinding(id(), Severity.ERROR,
                    "PII protection must require encryption and cryptographic shredding.",
                    "governance.piiProtection"));
        }
        return findings;
    }
}
