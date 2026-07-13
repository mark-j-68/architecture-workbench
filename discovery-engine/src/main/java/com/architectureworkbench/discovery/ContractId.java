package com.architectureworkbench.discovery;

/** Stable repository-local identity for a discovered contract. */
public record ContractId(String value) {
    public ContractId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Contract id is required.");
        value = value.trim();
    }

    public static ContractId of(String value) { return new ContractId(value); }
}
