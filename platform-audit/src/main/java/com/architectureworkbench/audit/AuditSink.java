package com.architectureworkbench.audit;

import java.util.List;

public interface AuditSink {
    AuditEvent append(AuditAppendRequest request);
    AuditEvent append(ArchitectureEvent event);
    List<AuditEvent> entries();
    List<AuditEvent> entriesForScope(String scopeType, String scopeId);
    List<AuditEvent> entriesForSubject(String subjectRef);
}
