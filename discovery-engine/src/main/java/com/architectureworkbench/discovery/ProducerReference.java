package com.architectureworkbench.discovery;

import java.util.Objects;

public record ProducerReference(String module, String symbol, ContractId contractId, MessageChannel channel) {
    public ProducerReference {
        module = Objects.requireNonNullElse(module, ".");
        symbol = Objects.requireNonNullElse(symbol, "");
        contractId = Objects.requireNonNull(contractId, "contractId");
    }
}
