package com.architectureworkbench.audit;

import java.util.Map;

public record DiscoveryStarted(String discoveryRunId, String sourceType, String sourceReference) {
    public DiscoveryStarted {
        discoveryRunId = AuditEventRecord.required(discoveryRunId, "discoveryRunId");
        sourceType = AuditEventRecord.required(sourceType, "sourceType");
        sourceReference = AuditEventRecord.required(sourceReference, "sourceReference");
    }

    public Map<String, String> payload() {
        return Map.of("discoveryRunId", discoveryRunId, "sourceType", sourceType, "sourceReference", sourceReference);
    }
}
