package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.audit.ProjectionGenerated;
import java.time.Instant;
import java.util.List;

public class ProjectionService {
    private final ImmutableKnowledgeGraphAuditLog auditLog;

    public ProjectionService(ImmutableKnowledgeGraphAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public Projection generateProjection(ArchitectureKnowledgeGraph graph, ProjectionType type, String actorRef) {
        List<String> elementRefs = graph.elements().stream().map(element -> element.id().value()).toList();
        List<String> relationshipRefs = graph.relationships().stream().map(Relationship::id).toList();
        Projection projection = new Projection(
                type,
                Instant.now(),
                elementRefs,
                relationshipRefs,
                switch (type) {
                    case EVENT_STORMING -> eventStorming(graph);
                    case REACT_FLOW -> reactFlow(graph);
                    case C4 -> c4(graph);
                    case BPMN -> processProjection(graph);
                    case DMN -> decisionProjection(graph);
                    case ADR -> adrProjection(graph);
                    case OPENAPI -> openApiProjection(graph);
                    case AI_REVIEW -> aiReviewProjection(graph);
                }
        );
        ProjectionGenerated event = new ProjectionGenerated(graph.graphId(), type.name(), elementRefs.size());
        auditLog.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.PROJECTION_GENERATED,
                graph.graphId(),
                ArchitectureEventSource.ARCHITECTURE_KNOWLEDGE_GRAPH,
                Actor.human(actorRef),
                CausationId.newId("generate-projection"),
                CorrelationId.newId("projection"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                event.payload(),
                null
        ));
        return projection;
    }

    private static EventStormingProjection eventStorming(ArchitectureKnowledgeGraph graph) {
        return new EventStormingProjection(
                names(graph, ArchitectureElementType.DOMAIN_EVENT),
                names(graph, ArchitectureElementType.COMMAND),
                names(graph, ArchitectureElementType.AGGREGATE),
                names(graph, ArchitectureElementType.POLICY)
        );
    }

    private static ReactFlowProjection reactFlow(ArchitectureKnowledgeGraph graph) {
        return new ReactFlowProjection(
                graph.elements().stream()
                        .map(element -> new ReactFlowProjection.Node(element.id().value(), element.type().name(), element.name()))
                        .toList(),
                graph.relationships().stream()
                        .map(relationship -> new ReactFlowProjection.Edge(
                                relationship.id(),
                                relationship.sourceId().value(),
                                relationship.targetId().value(),
                                relationship.type().name(),
                                relationship.label()))
                        .toList()
        );
    }

    private static C4Projection c4(ArchitectureKnowledgeGraph graph) {
        return new C4Projection(
                names(graph, ArchitectureElementType.SYSTEM),
                names(graph, ArchitectureElementType.CONTAINER),
                names(graph, ArchitectureElementType.COMPONENT),
                relationshipSummaries(graph)
        );
    }

    private static BpmnProjection processProjection(ArchitectureKnowledgeGraph graph) {
        return new BpmnProjection(
                names(graph, ArchitectureElementType.COMMAND),
                names(graph, ArchitectureElementType.DOMAIN_EVENT),
                graph.relationships().stream()
                        .filter(rel -> rel.type() == RelationshipType.HANDLED_BY || rel.type() == RelationshipType.EMITS)
                        .map(Relationship::id)
                        .toList()
        );
    }

    private static DmnProjection decisionProjection(ArchitectureKnowledgeGraph graph) {
        return new DmnProjection(
                names(graph, ArchitectureElementType.DECISION),
                names(graph, ArchitectureElementType.POLICY),
                names(graph, ArchitectureElementType.RISK)
        );
    }

    private static AdrProjection adrProjection(ArchitectureKnowledgeGraph graph) {
        return new AdrProjection(
                graph.elementsOfType(ArchitectureElementType.ADR).stream()
                        .map(element -> new AdrProjection.AdrSummary(element.id().value(), element.name(), ((ADR) element).status()))
                        .toList(),
                names(graph, ArchitectureElementType.DECISION)
        );
    }

    private static OpenApiProjection openApiProjection(ArchitectureKnowledgeGraph graph) {
        return new OpenApiProjection(
                graph.graphId(),
                names(graph, ArchitectureElementType.CAPABILITY),
                names(graph, ArchitectureElementType.COMMAND),
                names(graph, ArchitectureElementType.EVIDENCE)
        );
    }

    private static ReviewBoardProjection aiReviewProjection(ArchitectureKnowledgeGraph graph) {
        return new ReviewBoardProjection(
                names(graph, ArchitectureElementType.ARCHITECTURE_REVIEW),
                names(graph, ArchitectureElementType.RISK),
                names(graph, ArchitectureElementType.DECISION),
                names(graph, ArchitectureElementType.EVIDENCE)
        );
    }

    private static List<String> names(ArchitectureKnowledgeGraph graph, ArchitectureElementType type) {
        return graph.elementsOfType(type).stream().map(ArchitectureElement::name).toList();
    }

    private static List<String> relationshipSummaries(ArchitectureKnowledgeGraph graph) {
        return graph.relationships().stream()
                .map(rel -> "%s:%s->%s".formatted(rel.type(), rel.sourceId().value(), rel.targetId().value()))
                .toList();
    }
}
