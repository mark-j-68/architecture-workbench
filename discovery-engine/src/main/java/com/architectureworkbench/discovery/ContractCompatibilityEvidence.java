package com.architectureworkbench.discovery;

import java.util.Objects;

public record ContractCompatibilityEvidence(ContractId contractId, String mode, String supersedes,
                                            boolean deprecated, DiscoveryConfidence confidence) {
    public ContractCompatibilityEvidence {
        contractId = Objects.requireNonNull(contractId, "contractId");
        mode = Objects.requireNonNullElse(mode, "");
        supersedes = Objects.requireNonNullElse(supersedes, "");
        confidence = Objects.requireNonNull(confidence, "confidence");
    }
}
