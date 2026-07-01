package com.architectureworkbench.discovery;

import java.util.Objects;
import java.util.UUID;

public record DiscoverySource(
        String sourceId,
        DiscoverySourceType type,
        String uri,
        String displayName
) {
    public DiscoverySource {
        sourceId = Objects.requireNonNullElseGet(sourceId, () -> "source-" + UUID.randomUUID());
        type = Objects.requireNonNull(type, "type");
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Discovery source uri is required.");
        }
        displayName = Objects.requireNonNullElse(displayName, uri);
    }
}
