package com.architectureworkbench.audit;

import java.util.Map;
import java.util.Objects;

public record AuditAppendRequest(
        String scopeType,
        String scopeId,
        String actorRef,
        String action,
        String subjectRef,
        Map<String, String> details
) {
    public AuditAppendRequest {
        scopeType = AuditEventRecord.required(scopeType, "scopeType");
        scopeId = AuditEventRecord.required(scopeId, "scopeId");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        action = AuditEventRecord.required(action, "action");
        subjectRef = Objects.requireNonNullElse(subjectRef, "");
        details = Map.copyOf(Objects.requireNonNullElseGet(details, Map::of));
    }
}
