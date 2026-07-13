package com.architectureworkbench.discovery;

import java.util.List;
import java.util.Objects;

public record VersionComparison(String contractId, String producerVersion, String consumerVersion,
                                Result result, List<String> evidenceIds) {
    public enum Result { MATCH, MISMATCH, UNRESOLVED }
    public VersionComparison {
        contractId = Objects.requireNonNullElse(contractId, "unknown");
        producerVersion = Objects.requireNonNullElse(producerVersion, "");
        consumerVersion = Objects.requireNonNullElse(consumerVersion, "");
        result = Objects.requireNonNull(result, "result");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }
}
