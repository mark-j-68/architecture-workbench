package com.architectureworkbench.intelligence;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

final class AimIds {
    private AimIds() {}

    static String id(String prefix, String value) {
        return value == null || value.isBlank() ? prefix + "-" + UUID.randomUUID() : value;
    }

    static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    static double confidence(double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0.");
        }
        return value;
    }

    static <T> Collection<T> requireNonEmpty(Collection<T> values, String fieldName) {
        Objects.requireNonNull(values, fieldName);
        if (values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty.");
        }
        return values;
    }
}
