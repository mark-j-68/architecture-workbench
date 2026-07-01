package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.AuditAppendRequest;
import com.architectureworkbench.audit.ArchitectureEvent;
import com.architectureworkbench.audit.AuditEvent;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.audit.InMemoryAuditSink;
import java.util.List;
import java.util.Map;

public class ImmutableKnowledgeGraphAuditLog implements AuditSink {
    private final AuditSink delegate = new InMemoryAuditSink();

    public synchronized KnowledgeGraphAuditEvent append(String graphId, String actorRef, String action, String subjectRef, Map<String, String> details) {
        return toKnowledgeGraphEvent(delegate.append(new AuditAppendRequest("GRAPH", graphId, actorRef, action, subjectRef, details)));
    }

    @Override
    public AuditEvent append(AuditAppendRequest request) {
        return delegate.append(request);
    }

    @Override
    public AuditEvent append(ArchitectureEvent event) {
        return delegate.append(event);
    }

    @Override
    public List<AuditEvent> entries() {
        return delegate.entries();
    }

    @Override
    public List<AuditEvent> entriesForScope(String scopeType, String scopeId) {
        return delegate.entriesForScope(scopeType, scopeId);
    }

    @Override
    public List<AuditEvent> entriesForSubject(String subjectRef) {
        return delegate.entriesForSubject(subjectRef);
    }

    public synchronized List<KnowledgeGraphAuditEvent> entriesForGraph(String graphId) {
        List<AuditEvent> graphEvents = delegate.entriesForScope("GRAPH", graphId);
        List<AuditEvent> architectureEvents = delegate.entriesForScope("ARCHITECTURE", graphId);
        return java.util.stream.Stream.concat(graphEvents.stream(), architectureEvents.stream())
                .map(ImmutableKnowledgeGraphAuditLog::toKnowledgeGraphEvent)
                .toList();
    }

    private static KnowledgeGraphAuditEvent toKnowledgeGraphEvent(AuditEvent event) {
        return new KnowledgeGraphAuditEvent(
                event.eventId(),
                event.occurredAt(),
                event.actorRef(),
                event.action(),
                event.scopeId(),
                event.subjectRef(),
                event.details(),
                event.previousHash(),
                event.eventHash()
        );
    }
}
