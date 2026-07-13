package com.architectureworkbench.discovery;

import java.util.Objects;

public record ContractOwnershipEvidence(ContractId contractId, String owner, String sourceKind,
                                        boolean directlyDeclared, DiscoveryConfidence confidence) {
    public ContractOwnershipEvidence {
        contractId = Objects.requireNonNull(contractId, "contractId");
        owner = Objects.requireNonNullElse(owner, "");
        sourceKind = Objects.requireNonNullElse(sourceKind, "");
        confidence = Objects.requireNonNull(confidence, "confidence");
    }
}
