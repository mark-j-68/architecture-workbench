package com.architectureworkbench.discovery;

import java.util.Objects;

public record DependencyNode(String id, String kind, String module) {
    public DependencyNode {
        id = required(id, "id");
        kind = Objects.requireNonNullElse(kind, "unknown");
        module = Objects.requireNonNullElse(module, ".");
    }
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value;
    }
}
