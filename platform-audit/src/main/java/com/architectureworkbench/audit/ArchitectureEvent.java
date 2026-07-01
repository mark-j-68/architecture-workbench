package com.architectureworkbench.audit;

import java.time.Instant;
import java.util.Map;

public interface ArchitectureEvent {
    String eventId();
    ArchitectureEventType eventType();
    String workspaceId();
    ArchitectureEventSource source();
    Actor actor();
    CausationId causationId();
    CorrelationId correlationId();
    Instant timestamp();
    AuditRelevance auditRelevance();
    MutationTarget mutationTarget();
    Map<String, String> payload();
    String protectedPayloadReference();
}
