package com.architectureworkbench.audit;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(String value) {
    public CorrelationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("correlationId is required.");
        }
    }

    public static CorrelationId newId(String prefix) {
        return new CorrelationId("%s-%s".formatted(Objects.requireNonNullElse(prefix, "correlation"), UUID.randomUUID()));
    }
}
