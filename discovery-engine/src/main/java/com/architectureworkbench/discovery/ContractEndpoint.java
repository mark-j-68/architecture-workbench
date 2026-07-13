package com.architectureworkbench.discovery;

import java.util.Objects;

public record ContractEndpoint(ContractId contractId, String path, String operationId, String method) {
    public ContractEndpoint {
        contractId = Objects.requireNonNull(contractId, "contractId");
        path = Objects.requireNonNullElse(path, "");
        operationId = Objects.requireNonNullElse(operationId, "");
        method = Objects.requireNonNullElse(method, "").toUpperCase();
    }
}
