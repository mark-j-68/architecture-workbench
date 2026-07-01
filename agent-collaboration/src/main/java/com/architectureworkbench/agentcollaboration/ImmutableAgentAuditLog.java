package com.architectureworkbench.agentcollaboration;

import com.architectureworkbench.audit.AuditAppendRequest;
import com.architectureworkbench.audit.ArchitectureEvent;
import com.architectureworkbench.audit.AuditEvent;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.audit.InMemoryAuditSink;
import java.util.List;
import java.util.Map;

public class ImmutableAgentAuditLog implements AuditSink {
    private final AuditSink delegate = new InMemoryAuditSink();

    public synchronized AgentAuditEvent append(String workspaceId, String actorRef, String action, String subjectRef, Map<String, String> details) {
        return toAgentEvent(delegate.append(new AuditAppendRequest("REVIEW", workspaceId, actorRef, action, subjectRef, details)));
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

    public synchronized List<AgentAuditEvent> entriesForReview(String reviewId) {
        return delegate.entriesForSubject(reviewId).stream().map(ImmutableAgentAuditLog::toAgentEvent).toList();
    }

    private static AgentAuditEvent toAgentEvent(AuditEvent event) {
        return new AgentAuditEvent(
                event.eventId(),
                event.occurredAt(),
                event.scopeId(),
                event.actorRef(),
                event.action(),
                event.subjectRef(),
                event.details(),
                event.previousHash(),
                event.eventHash()
        );
    }
}
