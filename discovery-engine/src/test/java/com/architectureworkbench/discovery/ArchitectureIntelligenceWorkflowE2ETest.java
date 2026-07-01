package com.architectureworkbench.discovery;

import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditEvent;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.LifecycleStatus;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.intelligence.Severity;
import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.ProposedChangeStatus;
import com.architectureworkbench.knowledgegraph.ProposedElementAddition;
import com.architectureworkbench.knowledgegraph.ProposedRelationshipAddition;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import com.architectureworkbench.knowledgegraph.RelationshipType;
import com.architectureworkbench.reviewboard.ReviewBoardDecisionType;
import com.architectureworkbench.reviewboard.ReviewBoardParticipant;
import com.architectureworkbench.reviewboard.ReviewBoardParticipantType;
import com.architectureworkbench.reviewboard.ReviewBoardSession;
import com.architectureworkbench.reviewboard.ReviewBoardVote;
import com.architectureworkbench.reviewboard.ReviewBoardVoteType;
import com.architectureworkbench.reviewboard.ReviewBoardWorkflowService;
import com.architectureworkbench.workspace.InMemoryArchitectureGraphRepository;
import com.architectureworkbench.workspace.InMemoryWorkspaceRepository;
import com.architectureworkbench.workspace.Workspace;
import com.architectureworkbench.workspace.WorkspaceMetadata;
import com.architectureworkbench.workspace.WorkspaceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIntelligenceWorkflowE2ETest {
    @TempDir
    Path tempDir;

    @Test
    void discoveryReviewBoardAcceptanceAndExplicitProposalAcceptanceMutatesGraph() throws IOException {
        createRepresentativeSpringProject(tempDir);
        Fixture fixture = new Fixture();

        DiscoveryRun run = fixture.discoveryService.runDiscovery(fixture.discoveryContext(tempDir));

        assertEvidenceObservationFindingTraceability(run);
        assertFalse(run.recommendations().isEmpty());
        assertTrue(run.proposedChanges().size() >= 2);
        ProposedArchitectureChange firstElementChange = run.proposedChanges().get(0);
        ProposedArchitectureChange secondElementChange = run.proposedChanges().get(1);
        assertProposalTraceability(firstElementChange, run.recommendations(), run.aimFindings(), run.evidence());
        assertEquals(fixture.graph.graphId(), firstElementChange.workspaceId());
        assertFalse(firstElementChange.correlationId().value().isBlank());

        ReviewBoardSession session = fixture.openReviewBoard(run, List.of(firstElementChange, secondElementChange));
        session = fixture.reviewBoard.recordVote(session, vote("architect", ReviewBoardVoteType.APPROVE, "Discovery evidence is sufficient."));
        session = fixture.reviewBoard.recordVote(session, vote("ddd", ReviewBoardVoteType.APPROVE, "Boundary proposal is coherent."));
        session = fixture.reviewBoard.closeSession(session, "lead-architect");

        assertEquals(ReviewBoardDecisionType.ACCEPT_PROPOSED_CHANGE, session.decision().decisionType());
        assertEquals(firstElementChange.correlationId(), session.correlationId());
        assertFalse(session.votes().isEmpty());

        ProposedArchitectureChange acceptedFirst = fixture.proposedChangeService.acceptProposedChange(
                fixture.graph,
                firstElementChange,
                "lead-architect",
                "Accepted after review board approval."
        );
        ProposedArchitectureChange acceptedSecond = fixture.proposedChangeService.acceptProposedChange(
                fixture.graph,
                secondElementChange,
                "lead-architect",
                "Accepted after review board approval."
        );
        assertEquals(ProposedChangeStatus.ACCEPTED, acceptedFirst.status());
        assertEquals(ProposedChangeStatus.ACCEPTED, acceptedSecond.status());
        assertEquals(2, fixture.graph.elements().size());

        List<ArchitectureElement> elements = new ArrayList<>(fixture.graph.elements());
        ProposedArchitectureChange relationshipChange = fixture.proposedChangeService.proposeRelationshipAddition(
                fixture.graph.graphId(),
                firstElementChange.correlationId(),
                new ProposedRelationshipAddition(
                        elements.get(0).id(),
                        elements.get(1).id(),
                        RelationshipType.TRACES_TO,
                        "discovered with",
                        Map.of("source", "e2e")
                ),
                run.recommendations().get(0).id(),
                List.of(run.aimFindings().get(0).id()),
                List.of(run.evidence().get(0).id())
        );
        assertProposalTraceability(relationshipChange, run.recommendations(), run.aimFindings(), run.evidence());

        ProposedArchitectureChange acceptedRelationship = fixture.proposedChangeService.acceptProposedChange(
                fixture.graph,
                relationshipChange,
                "lead-architect",
                "Accepted after related element additions."
        );

        assertEquals(ProposedChangeStatus.ACCEPTED, acceptedRelationship.status());
        assertEquals(1, fixture.graph.relationships().size());
        assertArchitectureEvent(fixture.graphAudit.entries(), ArchitectureEventType.ELEMENT_ADDED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.graphAudit.entries(), ArchitectureEventType.RELATIONSHIP_ADDED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.WORKSPACE_CREATED, fixture.workspace.id().value());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.DISCOVERY_STARTED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.DISCOVERY_COMPLETED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.REVIEW_REQUESTED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.REVIEW_COMPLETED, fixture.graph.graphId());
        assertTypedEventMetadata(fixture.kernelAudit.entries());
        assertTypedEventMetadata(fixture.graphAudit.entries());
        assertHashChain(fixture.kernelAudit.entries());
        assertHashChain(fixture.graphAudit.entries());
    }

    @Test
    void rejectedReviewBoardDecisionDoesNotMutateGraphAndRemainsTraceable() {
        Fixture fixture = new Fixture();
        TraceableProposal traceable = fixture.traceableProposal();

        ReviewBoardSession session = fixture.reviewBoard.openSession(
                fixture.graph.graphId(),
                traceable.proposedChange().correlationId(),
                List.of(traceable.recommendation()),
                List.of(traceable.proposedChange()),
                "lead-architect"
        );
        session = addDefaultParticipants(fixture.reviewBoard, session);
        session = fixture.reviewBoard.recordVote(session, vote("architect", ReviewBoardVoteType.REJECT, "Evidence does not justify the change."));
        session = fixture.reviewBoard.recordVote(session, vote("ddd", ReviewBoardVoteType.REJECT, "The boundary is too speculative."));
        session = fixture.reviewBoard.closeSession(session, "lead-architect");

        ProposedArchitectureChange rejected = fixture.proposedChangeService.rejectProposedChange(
                traceable.proposedChange(),
                session.decision().rationale()
        );

        assertEquals(ReviewBoardDecisionType.REJECT_PROPOSED_CHANGE, session.decision().decisionType());
        assertEquals(ProposedChangeStatus.REJECTED, rejected.status());
        assertTrue(fixture.graph.elements().isEmpty());
        assertTrue(fixture.graph.relationships().isEmpty());
        assertTrue(fixture.graphAudit.entries().isEmpty());
        assertEquals(traceable.recommendation().id(), rejected.recommendationId());
        assertTrue(rejected.findingIds().contains(traceable.finding().id()));
        assertTrue(rejected.evidenceIds().contains(traceable.evidence().id()));
        assertTrue(session.recommendationCandidates().contains(traceable.recommendation()));
        assertTrue(session.proposedChanges().contains(traceable.proposedChange()));
        assertEquals(2, session.votes().size());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.REVIEW_REQUESTED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.REVIEW_COMPLETED, fixture.graph.graphId());
        assertHashChain(fixture.kernelAudit.entries());
    }

    @Test
    void requestMoreEvidenceDecisionLeavesDiscoveryFindingsUnapplied() throws IOException {
        createSparseProject(tempDir);
        Fixture fixture = new Fixture();

        DiscoveryRun run = fixture.discoveryService.runDiscovery(fixture.discoveryContext(tempDir));
        assertFalse(run.aimFindings().isEmpty());
        assertFalse(run.recommendations().isEmpty());
        assertFalse(run.proposedChanges().isEmpty());

        ReviewBoardSession session = fixture.openReviewBoard(run, List.of(run.proposedChanges().get(0)));
        session = fixture.reviewBoard.recordVote(session, vote("architect", ReviewBoardVoteType.REQUEST_MORE_EVIDENCE, "Need runtime ownership and API evidence."));
        session = fixture.reviewBoard.closeSession(session, "lead-architect");

        assertEquals(ReviewBoardDecisionType.REQUEST_FURTHER_DISCOVERY, session.decision().decisionType());
        assertTrue(fixture.graph.elements().isEmpty());
        assertTrue(fixture.graph.relationships().isEmpty());
        assertTrue(fixture.graphAudit.entries().isEmpty());
        assertEquals(run.proposedChanges().get(0).recommendationId(), run.recommendations().get(0).id());
        assertProposalTraceability(run.proposedChanges().get(0), run.recommendations(), run.aimFindings(), run.evidence());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.DISCOVERY_STARTED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.DISCOVERY_COMPLETED, fixture.graph.graphId());
        assertArchitectureEvent(fixture.kernelAudit.entries(), ArchitectureEventType.REVIEW_COMPLETED, fixture.graph.graphId());
        assertTypedEventMetadata(fixture.kernelAudit.entries());
        assertHashChain(fixture.kernelAudit.entries());
    }

    private static void assertEvidenceObservationFindingTraceability(DiscoveryRun run) {
        assertFalse(run.evidence().isEmpty());
        assertFalse(run.observations().isEmpty());
        assertFalse(run.aimFindings().isEmpty());
        assertTrue(run.observations().stream().allMatch(observation -> !observation.relatedEvidence().isEmpty()));
        assertTrue(run.aimFindings().stream().allMatch(finding -> !finding.supportingObservations().isEmpty()));
        assertTrue(run.recommendations().stream().allMatch(recommendation -> !recommendation.supportingFindings().isEmpty()));
        assertTrue(run.aimFindings().stream()
                .flatMap(finding -> finding.supportingObservations().stream())
                .flatMap(observation -> observation.relatedEvidence().stream())
                .anyMatch(run.evidence()::contains));
    }

    private static void assertProposalTraceability(
            ProposedArchitectureChange change,
            List<Recommendation> recommendations,
            List<Finding> findings,
            List<Evidence> evidence
    ) {
        assertTrue(recommendations.stream().anyMatch(recommendation -> recommendation.id().equals(change.recommendationId())));
        assertTrue(change.findingIds().stream()
                .anyMatch(findingId -> findings.stream().anyMatch(finding -> finding.id().equals(findingId))));
        assertTrue(change.evidenceIds().stream()
                .anyMatch(evidenceId -> evidence.stream().anyMatch(item -> item.id().equals(evidenceId))));
    }

    private static void assertArchitectureEvent(List<AuditEvent> events, ArchitectureEventType type, String workspaceId) {
        assertTrue(events.stream()
                .filter(event -> event.architectureEvent() != null)
                .anyMatch(event -> event.architectureEvent().eventType() == type
                        && event.architectureEvent().workspaceId().equals(workspaceId)),
                "Expected " + type.eventName() + " for workspace " + workspaceId);
    }

    private static void assertTypedEventMetadata(List<AuditEvent> events) {
        events.stream()
                .filter(event -> event.architectureEvent() != null)
                .forEach(event -> {
                    assertFalse(event.architectureEvent().workspaceId().isBlank());
                    assertFalse(event.architectureEvent().causationId().value().isBlank());
                    assertFalse(event.architectureEvent().correlationId().value().isBlank());
                    assertNotNull(event.architectureEvent().auditRelevance());
                    assertNotNull(event.architectureEvent().mutationTarget());
                });
    }

    private static void assertHashChain(List<AuditEvent> events) {
        for (int index = 1; index < events.size(); index++) {
            assertEquals(events.get(index - 1).eventHash(), events.get(index).previousHash());
        }
    }

    private static ReviewBoardSession addDefaultParticipants(ReviewBoardWorkflowService reviewBoard, ReviewBoardSession session) {
        session = reviewBoard.addParticipant(session, new ReviewBoardParticipant(
                "architect",
                "Lead Architect",
                ReviewBoardParticipantType.HUMAN_ARCHITECT
        ));
        return reviewBoard.addParticipant(session, new ReviewBoardParticipant(
                "ddd",
                "DDD Reviewer",
                ReviewBoardParticipantType.DDD_REVIEWER
        ));
    }

    private static ReviewBoardVote vote(String participantId, ReviewBoardVoteType type, String rationale) {
        return new ReviewBoardVote(null, participantId, type, rationale, null);
    }

    private static void createRepresentativeSpringProject(Path root) throws IOException {
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                  <modules>
                    <module>app</module>
                    <module>domain</module>
                  </modules>
                </project>
                """);
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app/api"));
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app/service"));
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app/repository"));
        Files.createDirectories(root.resolve("app/src/main/resources"));
        Files.createDirectories(root.resolve("app/src/test/java/com/example/app"));
        Files.createDirectories(root.resolve("domain/src/main/java/com/example/domain"));
        Files.createDirectories(root.resolve("architecture/adr"));
        Files.createDirectories(root.resolve("docs"));
        Files.writeString(root.resolve("app/pom.xml"), "<project></project>");
        Files.writeString(root.resolve("domain/pom.xml"), "<project></project>");
        Files.writeString(root.resolve("README.md"), "# Sample Spring Project\n");
        Files.writeString(root.resolve("docs/overview.md"), "# Overview\n");
        Files.writeString(root.resolve("Dockerfile"), "FROM eclipse-temurin:21\n");
        Files.writeString(root.resolve("app/src/main/resources/application.yml"), "server:\n  port: 8080\n");
        Files.writeString(root.resolve("app/src/main/java/com/example/app/api/CustomerController.java"), """
                package com.example.app.api;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class CustomerController {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/service/CustomerService.java"), """
                package com.example.app.service;
                import org.springframework.stereotype.Service;
                @Service
                class CustomerService {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/repository/CustomerRepository.java"), """
                package com.example.app.repository;
                class CustomerRepository {}
                """);
        Files.writeString(root.resolve("domain/src/main/java/com/example/domain/Customer.java"), """
                package com.example.domain;
                class Customer {}
                """);
    }

    private static void createSparseProject(Path root) throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project></project>");
        Files.createDirectories(root.resolve("src/main/java/com/example"));
        Files.writeString(root.resolve("src/main/java/com/example/App.java"), """
                package com.example;
                class App {}
                """);
    }

    private static class Fixture {
        final InMemoryAuditSink kernelAudit = new InMemoryAuditSink();
        final ImmutableKnowledgeGraphAuditLog graphAudit = new ImmutableKnowledgeGraphAuditLog();
        final WorkspaceService workspaceService = new WorkspaceService(
                new InMemoryWorkspaceRepository(),
                new InMemoryArchitectureGraphRepository(),
                kernelAudit
        );
        final Workspace workspace = workspaceService.createWorkspace("E2E Architecture", WorkspaceMetadata.empty(), "lead-architect");
        final ArchitectureKnowledgeGraph graph = workspaceService.getWorkspaceGraph(workspace.id());
        final ArchitectureElementService elementService = new ArchitectureElementService(graphAudit);
        final RelationshipService relationshipService = new RelationshipService(graphAudit);
        final ProposedChangeService proposedChangeService = new ProposedChangeService(elementService, relationshipService);
        final DiscoveryService discoveryService = new DiscoveryService(
                List.of(new LocalRepositoryDiscoveryConnector()),
                new DiscoveryGraphMapper(elementService),
                new HealthcheckService(kernelAudit),
                kernelAudit,
                proposedChangeService
        );
        final ReviewBoardWorkflowService reviewBoard = new ReviewBoardWorkflowService(kernelAudit);

        DiscoveryContext discoveryContext(Path root) {
            return new DiscoveryContext(
                    DiscoveryRunId.newId(),
                    new DiscoverySource("local-e2e", DiscoverySourceType.LOCAL_REPOSITORY, root.toUri().toString(), "Local E2E Source"),
                    root,
                    graph,
                    "lead-architect"
            );
        }

        ReviewBoardSession openReviewBoard(DiscoveryRun run, List<ProposedArchitectureChange> changes) {
            ReviewBoardSession session = reviewBoard.openSession(
                    graph.graphId(),
                    changes.get(0).correlationId(),
                    run.recommendations(),
                    changes,
                    "lead-architect"
            );
            return addDefaultParticipants(reviewBoard, session);
        }

        TraceableProposal traceableProposal() {
            Evidence evidence = new Evidence("evidence-reject", "test", "manual", 0.8, Instant.now(), List.of("ref"), List.of("artifact"));
            Observation observation = new Observation("observation-reject", "test", "Manual observation", List.of(evidence), List.of());
            Finding finding = new Finding("finding-reject", Severity.WARNING, "BOUNDARY", "Boundary is unclear.", List.of(observation), 0.8);
            Recommendation recommendation = new Recommendation(
                    "recommendation-reject",
                    "Add bounded context",
                    "Finding suggests a new boundary, pending review.",
                    List.of(),
                    List.of(finding),
                    "Medium",
                    "Low",
                    0.8,
                    LifecycleStatus.PROPOSED
            );
            ProposedArchitectureChange proposedChange = proposedChangeService.proposeElementAddition(
                    graph.graphId(),
                    new CorrelationId("reject-correlation-1"),
                    new ProposedElementAddition(ArchitectureElementType.BOUNDED_CONTEXT, "Customer Context", "", Map.of()),
                    recommendation.id(),
                    List.of(finding.id()),
                    List.of(evidence.id())
            );
            return new TraceableProposal(evidence, observation, finding, recommendation, proposedChange);
        }
    }

    private record TraceableProposal(
            Evidence evidence,
            Observation observation,
            Finding finding,
            Recommendation recommendation,
            ProposedArchitectureChange proposedChange
    ) {
    }
}
