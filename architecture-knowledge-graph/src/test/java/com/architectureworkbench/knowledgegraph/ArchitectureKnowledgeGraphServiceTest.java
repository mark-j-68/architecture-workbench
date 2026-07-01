package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.CorrelationId;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureKnowledgeGraphServiceTest {
    @Test
    void createsAndLinksElementsThroughServicesWithImmutableAuditEvents() {
        Fixture fixture = new Fixture();

        ArchitectureElement context = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.BOUNDED_CONTEXT, "Mortgage Origination", Map.of()));
        ArchitectureElement aggregate = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.AGGREGATE, "MortgageApplication", Map.of("rootEntity", "MortgageApplication")));
        ArchitectureElement command = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.COMMAND, "SubmitMortgageApplication", Map.of()));

        fixture.relationships.linkElements(fixture.graph, new LinkElementsCommand(context.id(), aggregate.id(), RelationshipType.CONTAINS, "contains", Map.of(), "architect"));
        fixture.relationships.linkElements(fixture.graph, new LinkElementsCommand(command.id(), aggregate.id(), RelationshipType.HANDLED_BY, "handled by", Map.of(), "architect"));

        assertEquals(3, fixture.graph.elements().size());
        assertEquals(2, fixture.graph.relationships().size());
        assertEquals(5, fixture.auditLog.entriesForGraph("kg-test").size());
        assertFalse(fixture.auditLog.entries().get(1).previousHash().equals("GENESIS"));
    }

    @Test
    void rejectsInvalidRelationshipShape() {
        Fixture fixture = new Fixture();
        ArchitectureElement command = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.COMMAND, "SubmitMortgageApplication", Map.of()));
        ArchitectureElement event = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.DOMAIN_EVENT, "MortgageApplicationSubmitted", Map.of()));

        assertThrows(IllegalArgumentException.class, () -> fixture.relationships.linkElements(fixture.graph,
                new LinkElementsCommand(command.id(), event.id(), RelationshipType.HANDLED_BY, "invalid", Map.of(), "architect")));
    }

    @Test
    void validatesDddConsistency() {
        Fixture fixture = new Fixture();
        fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.AGGREGATE, "Application", Map.of()));
        fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.COMMAND, "SubmitApplication", Map.of()));

        ValidationReport report = new DddConsistencyValidationService().validate(fixture.graph);

        assertTrue(report.hasErrors());
        assertTrue(report.findings().stream().anyMatch(finding -> finding.ruleId().equals("KG-DDD-001")));
        assertTrue(report.findings().stream().anyMatch(finding -> finding.ruleId().equals("KG-DDD-003")));
    }

    @Test
    void generatesProjectionsFromCanonicalGraph() {
        Fixture fixture = new Fixture();
        fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.SYSTEM, "Architecture Workbench", Map.of()));
        fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.CONTAINER, "Knowledge Graph API", Map.of()));
        fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.COMMAND, "CreateArchitectureElement", Map.of()));

        Projection reactFlow = new ProjectionService(fixture.auditLog).generateProjection(fixture.graph, ProjectionType.REACT_FLOW, "architect");
        Projection c4 = new ProjectionService(fixture.auditLog).generateProjection(fixture.graph, ProjectionType.C4, "architect");
        Projection openApi = new ProjectionService(fixture.auditLog).generateProjection(fixture.graph, ProjectionType.OPENAPI, "architect");

        assertEquals(ProjectionType.REACT_FLOW, reactFlow.type());
        assertTrue(reactFlow.payload() instanceof ReactFlowProjection);
        assertTrue(c4.payload() instanceof C4Projection);
        assertTrue(openApi.payload() instanceof OpenApiProjection);
        assertEquals(3, ((ReactFlowProjection) reactFlow.payload()).nodes().size());
        assertTrue(((C4Projection) c4.payload()).systems().contains("Architecture Workbench"));
        assertTrue(((OpenApiProjection) openApi.payload()).commandsAsOperations().contains("CreateArchitectureElement"));
    }

    @Test
    void recordsReviewFindingsAndTracesDecisionToEvidence() {
        Fixture fixture = new Fixture();
        ArchitectureElement decision = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.DECISION, "Use knowledge graph as source of truth", Map.of("outcome", "Accepted")));
        ArchitectureElement evidence = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.EVIDENCE, "ADR-001", Map.of("sourceUri", "architecture/adr/ADR-001.md")));

        ArchitectureReviewService reviewService = new ArchitectureReviewService(fixture.elements, fixture.relationships, fixture.auditLog);
        ArchitectureReview review = reviewService.recordReviewFinding(fixture.graph, new RecordReviewFindingCommand(
                decision.id(),
                "OpenAI/Codex",
                "Decision has evidence but should be linked explicitly.",
                "Trace the decision to ADR evidence.",
                "architect"
        ));

        DecisionTraceabilityService traceabilityService = new DecisionTraceabilityService(fixture.relationships, fixture.auditLog);
        traceabilityService.traceDecisionToEvidence(fixture.graph, decision.id(), evidence.id(), "architect");

        assertEquals(ArchitectureElementType.ARCHITECTURE_REVIEW, review.type());
        assertEquals(1, traceabilityService.evidenceForDecision(fixture.graph, decision.id()).size());
        assertTrue(fixture.auditLog.entriesForGraph("kg-test").stream().anyMatch(event -> event.action().equals("DECISION_TRACED_TO_EVIDENCE")));
    }

    @Test
    void proposedChangesOnlyMutateGraphWhenAccepted() {
        Fixture fixture = new Fixture();
        ProposedChangeService service = new ProposedChangeService(fixture.elements, fixture.relationships);
        ProposedArchitectureChange rejected = service.proposeElementAddition(
                "kg-test",
                new CorrelationId("discovery-correlation-1"),
                new ProposedElementAddition(ArchitectureElementType.COMPONENT, "Rejected Component", "", Map.of()),
                "recommendation-1",
                java.util.List.of("finding-1"),
                java.util.List.of("evidence-1")
        );
        ProposedArchitectureChange deferred = service.proposeElementAddition(
                "kg-test",
                new CorrelationId("discovery-correlation-1"),
                new ProposedElementAddition(ArchitectureElementType.COMPONENT, "Deferred Component", "", Map.of()),
                "recommendation-1",
                java.util.List.of("finding-1"),
                java.util.List.of("evidence-1")
        );
        ProposedArchitectureChange accepted = service.proposeElementAddition(
                "kg-test",
                new CorrelationId("discovery-correlation-1"),
                new ProposedElementAddition(ArchitectureElementType.COMPONENT, "Accepted Component", "", Map.of()),
                "recommendation-1",
                java.util.List.of("finding-1"),
                java.util.List.of("evidence-1")
        );

        rejected = service.rejectProposedChange(rejected, "Not relevant.");
        deferred = service.deferProposedChange(deferred, "Needs owner review.");

        assertEquals(ProposedChangeStatus.REJECTED, rejected.status());
        assertEquals(ProposedChangeStatus.DEFERRED, deferred.status());
        assertTrue(fixture.graph.elements().isEmpty());

        accepted = service.acceptProposedChange(fixture.graph, accepted, "architect", "Accepted after review.");

        assertEquals(ProposedChangeStatus.ACCEPTED, accepted.status());
        assertEquals(1, fixture.graph.elements().size());
        assertEquals("recommendation-1", accepted.recommendationId());
        assertTrue(accepted.findingIds().contains("finding-1"));
        assertTrue(accepted.evidenceIds().contains("evidence-1"));
        assertTrue(fixture.auditLog.entriesForGraph("kg-test").stream().anyMatch(event -> event.action().equals("ElementAdded")));
    }

    @Test
    void acceptedRelationshipProposalMutatesThroughRelationshipServiceAndEmitsTypedEvent() {
        Fixture fixture = new Fixture();
        ProposedChangeService service = new ProposedChangeService(fixture.elements, fixture.relationships);
        ArchitectureElement system = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.SYSTEM, "System", Map.of()));
        ArchitectureElement component = fixture.elements.createElement(fixture.graph, command(ArchitectureElementType.COMPONENT, "Component", Map.of()));
        ProposedArchitectureChange relationship = service.proposeRelationshipAddition(
                "kg-test",
                new CorrelationId("review-correlation-1"),
                new ProposedRelationshipAddition(system.id(), component.id(), RelationshipType.TRACES_TO, "trace", Map.of()),
                "recommendation-2",
                java.util.List.of("finding-2"),
                java.util.List.of("evidence-2")
        );

        relationship = service.acceptProposedChange(fixture.graph, relationship, "architect", "Accepted relationship.");

        assertEquals(ProposedChangeStatus.ACCEPTED, relationship.status());
        assertEquals(1, fixture.graph.relationships().size());
        assertTrue(fixture.auditLog.entriesForGraph("kg-test").stream().anyMatch(event -> event.action().equals("RelationshipAdded")));
    }

    private static CreateArchitectureElementCommand command(ArchitectureElementType type, String name, Map<String, String> attributes) {
        return new CreateArchitectureElementCommand(type, name, "", attributes, "architect");
    }

    private static class Fixture {
        final ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("kg-test");
        final ImmutableKnowledgeGraphAuditLog auditLog = new ImmutableKnowledgeGraphAuditLog();
        final ArchitectureElementService elements = new ArchitectureElementService(auditLog);
        final RelationshipService relationships = new RelationshipService(auditLog);
    }
}
