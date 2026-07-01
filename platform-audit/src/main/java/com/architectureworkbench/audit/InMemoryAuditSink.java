package com.architectureworkbench.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryAuditSink implements AuditSink {
    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public synchronized AuditEvent append(AuditAppendRequest request) {
        String previousHash = events.isEmpty() ? "GENESIS" : events.get(events.size() - 1).eventHash();
        String hashInput = "%s|%s|%s|%s|%s|%s|%s".formatted(
                previousHash,
                request.scopeType(),
                request.scopeId(),
                request.actorRef(),
                request.action(),
                request.subjectRef(),
                request.details()
        );
        AuditEvent event = new AuditEventRecord(
                null,
                Instant.now(),
                null,
                request.scopeType(),
                request.scopeId(),
                request.actorRef(),
                request.action(),
                request.subjectRef(),
                request.details(),
                previousHash,
                sha256(hashInput)
        );
        events.add(event);
        return event;
    }

    @Override
    public synchronized AuditEvent append(ArchitectureEvent architectureEvent) {
        ArchitectureEventEnvelope envelope = envelope(architectureEvent);
        Map<String, String> details = eventDetails(envelope);
        String previousHash = events.isEmpty() ? "GENESIS" : events.get(events.size() - 1).eventHash();
        String hashInput = "%s|%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                previousHash,
                envelope.eventId(),
                envelope.eventType().eventName(),
                envelope.workspaceId(),
                envelope.actor().actorRef(),
                envelope.causationId().value(),
                envelope.correlationId().value(),
                envelope.mutationTarget().name(),
                details
        );
        String subjectRef = subjectRef(details, envelope);
        AuditEvent event = new AuditEventRecord(
                null,
                envelope.timestamp(),
                envelope,
                "ARCHITECTURE",
                envelope.workspaceId(),
                envelope.actor().actorRef(),
                envelope.eventType().eventName(),
                subjectRef,
                details,
                previousHash,
                sha256(hashInput)
        );
        events.add(event);
        return event;
    }

    @Override
    public synchronized List<AuditEvent> entries() {
        return List.copyOf(events);
    }

    @Override
    public synchronized List<AuditEvent> entriesForScope(String scopeType, String scopeId) {
        return events.stream()
                .filter(event -> event.scopeType().equals(scopeType))
                .filter(event -> event.scopeId().equals(scopeId))
                .toList();
    }

    @Override
    public synchronized List<AuditEvent> entriesForSubject(String subjectRef) {
        return events.stream().filter(event -> event.subjectRef().equals(subjectRef)).toList();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required.", e);
        }
    }

    private static ArchitectureEventEnvelope envelope(ArchitectureEvent event) {
        if (event instanceof ArchitectureEventEnvelope typedEnvelope) {
            return typedEnvelope;
        }
        return new ArchitectureEventEnvelope(
                event.eventId(),
                event.eventType(),
                event.workspaceId(),
                event.source(),
                event.actor(),
                event.causationId(),
                event.correlationId(),
                event.timestamp(),
                event.auditRelevance(),
                event.mutationTarget(),
                event.payload(),
                event.protectedPayloadReference()
        );
    }

    private static Map<String, String> eventDetails(ArchitectureEventEnvelope event) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("eventType", event.eventType().eventName());
        details.put("source", event.source().name());
        details.put("actorType", event.actor().type().name());
        details.put("causationId", event.causationId().value());
        details.put("correlationId", event.correlationId().value());
        details.put("auditRelevance", event.auditRelevance().name());
        details.put("mutationTarget", event.mutationTarget().name());
        if (event.protectedPayloadReference() != null && !event.protectedPayloadReference().isBlank()) {
            details.put("protectedPayloadReference", event.protectedPayloadReference());
        }
        event.payload().forEach(details::putIfAbsent);
        return Map.copyOf(details);
    }

    private static String subjectRef(Map<String, String> details, ArchitectureEventEnvelope event) {
        for (String key : List.of(
                "subjectRef",
                "reviewId",
                "relationshipId",
                "elementId",
                "projectionType",
                "discoveryRunId",
                "sourceReference",
                "graphId",
                "workspaceId"
        )) {
            String value = details.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return event.workspaceId();
    }
}
