package com.architectureworkbench.agentcollaboration;

import com.architectureworkbench.audit.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AgentAuditEvent(
        String eventId,
        Instant occurredAt,
        String workspaceId,
        String actorRef,
        String action,
        String subjectRef,
        Map<String, String> details,
        String previousHash,
        String eventHash
) implements AuditEvent {
    public AgentAuditEvent {
        eventId = Objects.requireNonNullElseGet(eventId, () -> "agent-audit-" + UUID.randomUUID());
        occurredAt = Objects.requireNonNullElseGet(occurredAt, Instant::now);
        workspaceId = ReviewRequest.requireText(workspaceId, "workspaceId");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        action = ReviewRequest.requireText(action, "action");
        subjectRef = Objects.requireNonNullElse(subjectRef, "");
        details = Map.copyOf(Objects.requireNonNullElseGet(details, Map::of));
        previousHash = Objects.requireNonNullElse(previousHash, "GENESIS");
        eventHash = ReviewRequest.requireText(eventHash, "eventHash");
    }

    @Override public String scopeType() { return "REVIEW"; }
    @Override public String scopeId() { return workspaceId; }
}
