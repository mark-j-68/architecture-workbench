package com.architectureworkbench.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEventRecord(
        String eventId,
        Instant occurredAt,
        ArchitectureEventEnvelope architectureEvent,
        String scopeType,
        String scopeId,
        String actorRef,
        String action,
        String subjectRef,
        Map<String, String> details,
        String previousHash,
        String eventHash
) implements AuditEvent {
    public AuditEventRecord {
        eventId = Objects.requireNonNullElseGet(eventId, () -> "audit-" + UUID.randomUUID());
        occurredAt = Objects.requireNonNullElseGet(occurredAt, Instant::now);
        scopeType = required(scopeType, "scopeType");
        scopeId = required(scopeId, "scopeId");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        action = required(action, "action");
        subjectRef = Objects.requireNonNullElse(subjectRef, "");
        details = Map.copyOf(Objects.requireNonNullElseGet(details, Map::of));
        previousHash = Objects.requireNonNullElse(previousHash, "GENESIS");
        eventHash = required(eventHash, "eventHash");
    }

    static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
