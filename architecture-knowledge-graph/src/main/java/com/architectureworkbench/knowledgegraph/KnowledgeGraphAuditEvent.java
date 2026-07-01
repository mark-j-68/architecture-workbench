package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record KnowledgeGraphAuditEvent(
        String eventId,
        Instant occurredAt,
        String actorRef,
        String action,
        String graphId,
        String subjectRef,
        Map<String, String> details,
        String previousHash,
        String eventHash
) implements AuditEvent {
    public KnowledgeGraphAuditEvent {
        eventId = Objects.requireNonNullElseGet(eventId, () -> "kg-audit-" + UUID.randomUUID());
        occurredAt = Objects.requireNonNullElseGet(occurredAt, Instant::now);
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        action = ArchitectureElement.requireText(action, "action");
        graphId = ArchitectureElement.requireText(graphId, "graphId");
        subjectRef = Objects.requireNonNullElse(subjectRef, "");
        details = Map.copyOf(Objects.requireNonNullElseGet(details, Map::of));
        previousHash = Objects.requireNonNullElse(previousHash, "GENESIS");
        eventHash = ArchitectureElement.requireText(eventHash, "eventHash");
    }

    @Override public String scopeType() { return "GRAPH"; }
    @Override public String scopeId() { return graphId; }
}
