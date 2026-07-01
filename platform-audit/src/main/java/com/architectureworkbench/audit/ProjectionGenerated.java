package com.architectureworkbench.audit;

import java.util.Map;

public record ProjectionGenerated(String graphId, String projectionType, int elementCount) {
    public ProjectionGenerated {
        graphId = AuditEventRecord.required(graphId, "graphId");
        projectionType = AuditEventRecord.required(projectionType, "projectionType");
    }

    public Map<String, String> payload() {
        return Map.of("graphId", graphId, "projectionType", projectionType, "elementCount", String.valueOf(elementCount));
    }
}
