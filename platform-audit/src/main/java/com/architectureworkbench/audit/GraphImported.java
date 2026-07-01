package com.architectureworkbench.audit;

import java.util.Map;

public record GraphImported(String graphId, int elementCount, int relationshipCount) {
    public GraphImported {
        graphId = AuditEventRecord.required(graphId, "graphId");
    }

    public Map<String, String> payload() {
        return Map.of(
                "graphId", graphId,
                "elementCount", String.valueOf(elementCount),
                "relationshipCount", String.valueOf(relationshipCount)
        );
    }
}
