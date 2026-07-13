package com.architectureworkbench.discovery;

import java.util.Objects;

public record ConsumerReference(String module, String symbol, ContractId contractId, MessageChannel channel) {
    public ConsumerReference {
        module = Objects.requireNonNullElse(module, ".");
        symbol = Objects.requireNonNullElse(symbol, "");
        contractId = Objects.requireNonNull(contractId, "contractId");
    }
}
