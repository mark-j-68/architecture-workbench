package com.architectureworkbench.workspace;

import com.architectureworkbench.audit.ArchitectureEvent;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.AuditAppendRequest;
import com.architectureworkbench.audit.AuditEvent;
import com.architectureworkbench.audit.AuditEventRecord;
import com.architectureworkbench.audit.AuditSink;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FileAuditSink implements AuditSink {
    private static final TypeReference<List<AuditEventRecord>> AUDIT_LIST = new TypeReference<>() {};
    private final Path root;

    public FileAuditSink() {
        this(FileWorkspaceStorage.defaultRoot());
    }

    public FileAuditSink(Path root) {
        this.root = root;
    }

    @Override
    public synchronized AuditEvent append(AuditAppendRequest request) {
        List<AuditEventRecord> events = load(request.scopeId());
        String previousHash = previousHash(events);
        String hashInput = "%s|%s|%s|%s|%s|%s|%s".formatted(
                previousHash,
                request.scopeType(),
                request.scopeId(),
                request.actorRef(),
                request.action(),
                request.subjectRef(),
                request.details()
        );
        AuditEventRecord event = new AuditEventRecord(
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
        save(request.scopeId(), events);
        return event;
    }

    @Override
    public synchronized AuditEvent append(ArchitectureEvent architectureEvent) {
        ArchitectureEventEnvelope envelope = envelope(architectureEvent);
        List<AuditEventRecord> events = load(envelope.workspaceId());
        Map<String, String> details = eventDetails(envelope);
        String previousHash = previousHash(events);
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
        AuditEventRecord event = new AuditEventRecord(
                null,
                envelope.timestamp(),
                envelope,
                "ARCHITECTURE",
                envelope.workspaceId(),
                envelope.actor().actorRef(),
                envelope.eventType().eventName(),
                subjectRef(details, envelope),
                details,
                previousHash,
                sha256(hashInput)
        );
        events.add(event);
        save(envelope.workspaceId(), events);
        return event;
    }

    @Override
    public synchronized List<AuditEvent> entries() {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (java.util.stream.Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .flatMap(path -> load(path.getFileName().toString()).stream())
                    .map(AuditEvent.class::cast)
                    .toList();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to list audit storage directory: " + root, e);
        }
    }

    @Override
    public synchronized List<AuditEvent> entriesForScope(String scopeType, String scopeId) {
        return load(scopeId).stream()
                .filter(event -> event.scopeType().equals(scopeType))
                .map(AuditEvent.class::cast)
                .toList();
    }

    @Override
    public synchronized List<AuditEvent> entriesForSubject(String subjectRef) {
        return entries().stream().filter(event -> event.subjectRef().equals(subjectRef)).toList();
    }

    private List<AuditEventRecord> load(String workspaceId) {
        Path path = auditFile(workspaceId);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(WorkspaceJson.read(path, AUDIT_LIST));
    }

    private void save(String workspaceId, List<AuditEventRecord> events) {
        WorkspaceJson.write(auditFile(workspaceId), events);
    }

    private Path auditFile(String workspaceId) {
        return root.resolve(workspaceId).resolve("audit-events.json");
    }

    private static String previousHash(List<AuditEventRecord> events) {
        return events.isEmpty() ? "GENESIS" : events.get(events.size() - 1).eventHash();
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
}
