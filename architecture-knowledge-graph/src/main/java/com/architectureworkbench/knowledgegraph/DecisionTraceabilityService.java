package com.architectureworkbench.knowledgegraph;

import java.util.List;
import java.util.Map;

public class DecisionTraceabilityService {
    private final RelationshipService relationshipService;
    private final ImmutableKnowledgeGraphAuditLog auditLog;

    public DecisionTraceabilityService(RelationshipService relationshipService, ImmutableKnowledgeGraphAuditLog auditLog) {
        this.relationshipService = relationshipService;
        this.auditLog = auditLog;
    }

    public Relationship traceDecisionToEvidence(ArchitectureKnowledgeGraph graph, ElementId decisionId, ElementId evidenceId, String actorRef) {
        ArchitectureElement decision = graph.element(decisionId)
                .orElseThrow(() -> new IllegalArgumentException("Decision does not exist: " + decisionId.value()));
        ArchitectureElement evidence = graph.element(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence does not exist: " + evidenceId.value()));
        if (decision.type() != ArchitectureElementType.DECISION) {
            throw new IllegalArgumentException("Trace source must be a Decision.");
        }
        if (evidence.type() != ArchitectureElementType.EVIDENCE) {
            throw new IllegalArgumentException("Trace target must be Evidence.");
        }

        Relationship relationship = relationshipService.linkElements(graph, new LinkElementsCommand(
                decisionId,
                evidenceId,
                RelationshipType.EVIDENCED_BY,
                "evidenced by",
                Map.of(),
                actorRef
        ));
        auditLog.append(graph.graphId(), actorRef, "DECISION_TRACED_TO_EVIDENCE", decisionId.value(),
                Map.of("decision", decisionId.value(), "evidence", evidenceId.value()));
        return relationship;
    }

    public List<Evidence> evidenceForDecision(ArchitectureKnowledgeGraph graph, ElementId decisionId) {
        return graph.outgoing(decisionId, RelationshipType.EVIDENCED_BY).stream()
                .map(Relationship::targetId)
                .map(graph::element)
                .flatMap(java.util.Optional::stream)
                .filter(element -> element.type() == ArchitectureElementType.EVIDENCE)
                .map(Evidence.class::cast)
                .toList();
    }
}
