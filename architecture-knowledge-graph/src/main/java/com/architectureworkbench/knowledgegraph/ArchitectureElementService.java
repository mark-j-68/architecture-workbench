package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.ElementAdded;
import com.architectureworkbench.audit.MutationTarget;
import java.util.Map;

public class ArchitectureElementService {
    private final ArchitectureElementFactory factory = new ArchitectureElementFactory();
    private final ImmutableKnowledgeGraphAuditLog auditLog;

    public ArchitectureElementService(ImmutableKnowledgeGraphAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public ArchitectureElement createElement(ArchitectureKnowledgeGraph graph, CreateArchitectureElementCommand command) {
        ArchitectureElement element = factory.create(command.type(), command.name(), command.description(), command.attributes());
        graph.addElement(element);
        ElementAdded event = new ElementAdded(graph.graphId(), element.id().value(), element.type().name(), element.name());
        auditLog.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.ELEMENT_ADDED,
                graph.graphId(),
                ArchitectureEventSource.ARCHITECTURE_KNOWLEDGE_GRAPH,
                Actor.human(command.actorRef()),
                CausationId.newId("create-element"),
                CorrelationId.newId("graph"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.GRAPH,
                event.payload(),
                null
        ));
        return element;
    }
}
