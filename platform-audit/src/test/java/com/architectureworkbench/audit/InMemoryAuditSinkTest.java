package com.architectureworkbench.audit;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InMemoryAuditSinkTest {
    @Test
    void appendsHashChainedAuditEvents() {
        InMemoryAuditSink sink = new InMemoryAuditSink();

        sink.append(new AuditAppendRequest("GRAPH", "kg-1", "architect", "CREATED", "el-1", Map.of()));
        sink.append(new AuditAppendRequest("GRAPH", "kg-1", "architect", "LINKED", "rel-1", Map.of("type", "CONTAINS")));

        assertEquals(2, sink.entriesForScope("GRAPH", "kg-1").size());
        assertFalse(sink.entries().get(1).previousHash().equals("GENESIS"));
    }

    @Test
    void retainsTypedArchitectureEventsInHashChainedAuditRecords() {
        InMemoryAuditSink sink = new InMemoryAuditSink();

        sink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.ELEMENT_ADDED,
                "workspace-1",
                ArchitectureEventSource.ARCHITECTURE_KNOWLEDGE_GRAPH,
                Actor.human("architect"),
                new CausationId("create-element-command-1"),
                new CorrelationId("design-workflow-1"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.GRAPH,
                new ElementAdded("graph-1", "element-1", "COMPONENT", "API").payload(),
                "protected-payload-1"
        ));
        sink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.PROJECTION_GENERATED,
                "workspace-1",
                ArchitectureEventSource.ARCHITECTURE_KNOWLEDGE_GRAPH,
                Actor.human("architect"),
                new CausationId("generate-projection-command-1"),
                new CorrelationId("design-workflow-1"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                new ProjectionGenerated("graph-1", "C4", 1).payload(),
                null
        ));

        AuditEvent first = sink.entries().get(0);
        AuditEvent second = sink.entries().get(1);

        assertEquals("ElementAdded", first.action());
        assertEquals("workspace-1", first.scopeId());
        assertEquals("ARCHITECTURE", first.scopeType());
        assertNotNull(first.architectureEvent());
        assertEquals(ArchitectureEventType.ELEMENT_ADDED, first.architectureEvent().eventType());
        assertEquals("workspace-1", first.architectureEvent().workspaceId());
        assertEquals("create-element-command-1", first.architectureEvent().causationId().value());
        assertEquals("design-workflow-1", first.architectureEvent().correlationId().value());
        assertEquals(AuditRelevance.REQUIRED, first.architectureEvent().auditRelevance());
        assertEquals(MutationTarget.GRAPH, first.architectureEvent().mutationTarget());
        assertEquals("protected-payload-1", first.architectureEvent().protectedPayloadReference());
        assertEquals("ProjectionGenerated", second.action());
        assertFalse(second.previousHash().equals("GENESIS"));
    }
}
