package com.architectureworkbench.discovery;

import java.util.List;

public record DiscoveryResult(
        DiscoveryRunId runId,
        DiscoverySource source,
        List<DiscoveredArtifact> artifacts
) {
    public DiscoveryResult {
        artifacts = List.copyOf(artifacts == null ? List.of() : artifacts);
    }
}
