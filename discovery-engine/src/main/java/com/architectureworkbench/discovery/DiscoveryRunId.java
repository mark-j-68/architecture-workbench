package com.architectureworkbench.discovery;

import java.util.Objects;
import java.util.UUID;

public record DiscoveryRunId(String value) {
    public DiscoveryRunId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Discovery run id is required.");
        }
    }

    public static DiscoveryRunId newId() {
        return new DiscoveryRunId("discovery-run-" + UUID.randomUUID());
    }

    public static DiscoveryRunId of(String value) {
        return new DiscoveryRunId(Objects.requireNonNull(value));
    }
}
