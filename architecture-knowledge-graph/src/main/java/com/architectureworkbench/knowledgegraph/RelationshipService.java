package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.audit.RelationshipAdded;
import java.util.Map;

public class RelationshipService {
    private final ImmutableKnowledgeGraphAuditLog auditLog;

    public RelationshipService(ImmutableKnowledgeGraphAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public Relationship linkElements(ArchitectureKnowledgeGraph graph, LinkElementsCommand command) {
        ArchitectureElement source = graph.element(command.sourceId())
                .orElseThrow(() -> new IllegalArgumentException("Source element does not exist: " + command.sourceId().value()));
        ArchitectureElement target = graph.element(command.targetId())
                .orElseThrow(() -> new IllegalArgumentException("Target element does not exist: " + command.targetId().value()));
        validateRelationship(source, target, command.type());

        Relationship relationship = new Relationship(null, command.sourceId(), command.targetId(), command.type(), command.label(), command.attributes(), null);
        graph.addRelationship(relationship);
        RelationshipAdded event = new RelationshipAdded(
                graph.graphId(),
                relationship.id(),
                command.sourceId().value(),
                command.targetId().value(),
                command.type().name()
        );
        auditLog.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.RELATIONSHIP_ADDED,
                graph.graphId(),
                ArchitectureEventSource.ARCHITECTURE_KNOWLEDGE_GRAPH,
                Actor.human(command.actorRef()),
                CausationId.newId("link-elements"),
                CorrelationId.newId("graph"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.GRAPH,
                event.payload(),
                null
        ));
        return relationship;
    }

    private static void validateRelationship(ArchitectureElement source, ArchitectureElement target, RelationshipType type) {
        if (type == RelationshipType.HANDLED_BY && (source.type() != ArchitectureElementType.COMMAND || target.type() != ArchitectureElementType.AGGREGATE)) {
            throw new IllegalArgumentException("HANDLED_BY must link Command -> Aggregate.");
        }
        if (type == RelationshipType.EMITS && (source.type() != ArchitectureElementType.AGGREGATE || target.type() != ArchitectureElementType.DOMAIN_EVENT)) {
            throw new IllegalArgumentException("EMITS must link Aggregate -> DomainEvent.");
        }
        if (type == RelationshipType.EVIDENCED_BY && target.type() != ArchitectureElementType.EVIDENCE) {
            throw new IllegalArgumentException("EVIDENCED_BY must target Evidence.");
        }
        if (type == RelationshipType.DOCUMENTED_BY && target.type() != ArchitectureElementType.ADR) {
            throw new IllegalArgumentException("DOCUMENTED_BY must target ADR.");
        }
        if (type == RelationshipType.REVIEWED_BY && target.type() != ArchitectureElementType.ARCHITECTURE_REVIEW) {
            throw new IllegalArgumentException("REVIEWED_BY must target ArchitectureReview.");
        }
    }
}
