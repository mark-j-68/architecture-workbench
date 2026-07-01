package com.architectureworkbench.audit;

import java.util.Map;

public record ElementAdded(String graphId, String elementId, String elementType, String name) {
    public ElementAdded {
        graphId = AuditEventRecord.required(graphId, "graphId");
        elementId = AuditEventRecord.required(elementId, "elementId");
        elementType = AuditEventRecord.required(elementType, "elementType");
        name = AuditEventRecord.required(name, "name");
    }

    public Map<String, String> payload() {
        return Map.of("graphId", graphId, "elementId", elementId, "elementType", elementType, "name", name);
    }
}
