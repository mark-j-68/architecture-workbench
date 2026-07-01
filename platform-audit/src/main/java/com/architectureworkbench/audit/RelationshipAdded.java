package com.architectureworkbench.audit;

import java.util.Map;

public record RelationshipAdded(String graphId, String relationshipId, String sourceElementId, String targetElementId, String relationshipType) {
    public RelationshipAdded {
        graphId = AuditEventRecord.required(graphId, "graphId");
        relationshipId = AuditEventRecord.required(relationshipId, "relationshipId");
        sourceElementId = AuditEventRecord.required(sourceElementId, "sourceElementId");
        targetElementId = AuditEventRecord.required(targetElementId, "targetElementId");
        relationshipType = AuditEventRecord.required(relationshipType, "relationshipType");
    }

    public Map<String, String> payload() {
        return Map.of(
                "graphId", graphId,
                "relationshipId", relationshipId,
                "sourceElementId", sourceElementId,
                "targetElementId", targetElementId,
                "relationshipType", relationshipType
        );
    }
}
