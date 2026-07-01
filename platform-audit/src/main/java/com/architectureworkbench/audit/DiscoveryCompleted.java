package com.architectureworkbench.audit;

import java.util.Map;

public record DiscoveryCompleted(String discoveryRunId, int artifactCount, int findingCount) {
    public DiscoveryCompleted {
        discoveryRunId = AuditEventRecord.required(discoveryRunId, "discoveryRunId");
    }

    public Map<String, String> payload() {
        return Map.of(
                "discoveryRunId", discoveryRunId,
                "artifactCount", String.valueOf(artifactCount),
                "findingCount", String.valueOf(findingCount)
        );
    }
}
