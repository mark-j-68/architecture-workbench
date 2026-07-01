package com.architectureworkbench.discovery;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DiscoveredArtifact(
        String artifactId,
        DiscoveredArtifactType type,
        String path,
        String name,
        Map<String, String> metadata
) {
    public DiscoveredArtifact {
        artifactId = Objects.requireNonNullElseGet(artifactId, () -> "artifact-" + UUID.randomUUID());
        type = Objects.requireNonNull(type, "type");
        path = Objects.requireNonNullElse(path, "");
        name = Objects.requireNonNullElse(name, path);
        metadata = Map.copyOf(Objects.requireNonNullElseGet(metadata, Map::of));
    }
}
