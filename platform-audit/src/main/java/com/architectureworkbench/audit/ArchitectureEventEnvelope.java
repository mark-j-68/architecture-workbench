package com.architectureworkbench.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ArchitectureEventEnvelope(
        String eventId,
        ArchitectureEventType eventType,
        String workspaceId,
        ArchitectureEventSource source,
        Actor actor,
        CausationId causationId,
        CorrelationId correlationId,
        Instant timestamp,
        AuditRelevance auditRelevance,
        MutationTarget mutationTarget,
        Map<String, String> payload,
        String protectedPayloadReference
) implements ArchitectureEvent {
    public ArchitectureEventEnvelope {
        eventId = Objects.requireNonNullElseGet(eventId, () -> "architecture-event-" + UUID.randomUUID());
        eventType = Objects.requireNonNull(eventType, "eventType");
        workspaceId = required(workspaceId, "workspaceId");
        source = Objects.requireNonNull(source, "source");
        actor = Objects.requireNonNullElseGet(actor, () -> Actor.system("system"));
        causationId = Objects.requireNonNull(causationId, "causationId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId");
        timestamp = Objects.requireNonNullElseGet(timestamp, Instant::now);
        auditRelevance = Objects.requireNonNullElse(auditRelevance, AuditRelevance.REQUIRED);
        mutationTarget = Objects.requireNonNull(mutationTarget, "mutationTarget");
        payload = Map.copyOf(Objects.requireNonNullElseGet(payload, Map::of));
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
