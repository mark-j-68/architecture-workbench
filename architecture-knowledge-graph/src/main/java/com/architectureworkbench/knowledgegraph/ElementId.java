package com.architectureworkbench.knowledgegraph;

import java.util.Objects;
import java.util.UUID;

public record ElementId(String value) {
    public ElementId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Element id is required.");
        }
    }

    public static ElementId newId(ArchitectureElementType type) {
        return new ElementId(type.name().toLowerCase().replace('_', '-') + "-" + UUID.randomUUID());
    }

    public static ElementId of(String value) {
        return new ElementId(Objects.requireNonNull(value));
    }
}
