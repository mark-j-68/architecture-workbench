package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.Objects;

public record DiscoveryPluginExecutionRecord(
        DiscoveryPluginMetadata metadata,
        DiscoveryPluginResult result,
        Instant startedAt,
        Instant completedAt
) {
    public DiscoveryPluginExecutionRecord {
        metadata = Objects.requireNonNull(metadata, "metadata");
        result = Objects.requireNonNull(result, "result");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        completedAt = Objects.requireNonNull(completedAt, "completedAt");
    }
}
