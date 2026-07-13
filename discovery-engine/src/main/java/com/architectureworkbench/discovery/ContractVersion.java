package com.architectureworkbench.discovery;

import java.util.Objects;

public record ContractVersion(String value, boolean explicit, String source) {
    public ContractVersion {
        value = Objects.requireNonNullElse(value, "").trim();
        source = Objects.requireNonNullElse(source, "").trim();
    }
}
