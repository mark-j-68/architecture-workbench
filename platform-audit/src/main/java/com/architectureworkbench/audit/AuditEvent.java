package com.architectureworkbench.audit;

import java.time.Instant;
import java.util.Map;

public interface AuditEvent {
    String eventId();
    Instant occurredAt();
    default ArchitectureEventEnvelope architectureEvent() {
        return null;
    }
    String scopeType();
    String scopeId();
    String actorRef();
    String action();
    String subjectRef();
    Map<String, String> details();
    String previousHash();
    String eventHash();
}
