package com.architectureworkbench.audit;

import java.util.Objects;
import java.util.UUID;

public record CausationId(String value) {
    public CausationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("causationId is required.");
        }
    }

    public static CausationId newId(String prefix) {
        return new CausationId("%s-%s".formatted(Objects.requireNonNullElse(prefix, "cause"), UUID.randomUUID()));
    }
}
