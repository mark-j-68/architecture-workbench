package com.architectureworkbench.reviewboard;

import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.LifecycleStatus;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.intelligence.Severity;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.CreateArchitectureElementCommand;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.ProposedChangeStatus;
import com.architectureworkbench.knowledgegraph.ProposedElementAddition;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewBoardWorkflowServiceTest {
    @Test
    void opensSessionForRecommendationsAndProposedChangesAndEmitsReviewRequested() {
        Fixture fixture = new Fixture();

        ReviewBoardSession session = fixture.workflow.openSession(
                "workspace-review",
                fixture.correlationId,
                List.of(fixture.recommendation),
                List.of(fixture.proposedChange),
                "lead-architect"
        );

        assertEquals(ReviewBoardSessionStatus.OPEN, session.status());
        assertEquals(1, session.recommendationCandidates().size());
        assertEquals(1, session.proposedChanges().size());
        assertEquals("ReviewRequested", fixture.auditSink.entries().get(0).action());
        assertEquals("workspace-review", fixture.auditSink.entries().get(0).architectureEvent().workspaceId());
        assertEquals("review-board-correlation-1", fixture.auditSink.entries().get(0).architectureEvent().correlationId().value());
    }

    @Test
    void unanimousApprovalRecommendsAcceptanceWithoutGraphMutationAndEmitsReviewCompleted() {
        Fixture fixture = new Fixture();
        ReviewBoardSession session = fixture.openWithParticipants();

        session = fixture.workflow.recordVote(session, vote("architect", ReviewBoardVoteType.APPROVE, "Looks ready."));
        session = fixture.workflow.recordVote(session, vote("security", ReviewBoardVoteType.APPROVE, "Controls are adequate."));
        session = fixture.workflow.closeSession(session, "lead-architect");

        assertEquals(ReviewBoardSessionStatus.CLOSED, session.status());
        assertEquals(ReviewBoardDecisionType.ACCEPT_PROPOSED_CHANGE, session.decision().decisionType());
        assertEquals(1, fixture.graph.elements().size());
        assertTrue(fixture.graph.relationships().isEmpty());
        assertEquals(ProposedChangeStatus.PROPOSED, fixture.proposedChange.status());
        assertEquals("ReviewCompleted", fixture.auditSink.entries().get(1).action());
        assertEquals("workspace-review", fixture.auditSink.entries().get(1).architectureEvent().workspaceId());
        assertEquals("review-board-correlation-1", fixture.auditSink.entries().get(1).architectureEvent().correlationId().value());
    }

    @Test
    void conflictingVotesProduceDeferredDecision() {
        Fixture fixture = new Fixture();
        ReviewBoardSession session = fixture.openWithParticipants();

        session = fixture.workflow.recordVote(session, vote("architect", ReviewBoardVoteType.APPROVE, "Accept."));
        session = fixture.workflow.recordVote(session, vote("security", ReviewBoardVoteType.REJECT, "Security evidence is incomplete."));

        ReviewBoardDecision decision = fixture.workflow.deriveReviewBoardDecision(session);

        assertEquals(ReviewBoardDecisionType.DEFER_PROPOSED_CHANGE, decision.decisionType());
    }

    @Test
    void rejectionVotesRecommendRejection() {
        Fixture fixture = new Fixture();
        ReviewBoardSession session = fixture.openWithParticipants();

        session = fixture.workflow.recordVote(session, vote("architect", ReviewBoardVoteType.REJECT, "Wrong boundary."));
        session = fixture.workflow.recordVote(session, vote("security", ReviewBoardVoteType.REJECT, "Insufficient controls."));

        ReviewBoardDecision decision = fixture.workflow.deriveReviewBoardDecision(session);

        assertEquals(ReviewBoardDecisionType.REJECT_PROPOSED_CHANGE, decision.decisionType());
    }

    @Test
    void requestMoreEvidenceVotesRecommendFurtherDiscovery() {
        Fixture fixture = new Fixture();
        ReviewBoardSession session = fixture.openWithParticipants();

        session = fixture.workflow.recordVote(session, vote("architect", ReviewBoardVoteType.REQUEST_MORE_EVIDENCE, "Need runtime dependency evidence."));

        ReviewBoardDecision decision = fixture.workflow.deriveReviewBoardDecision(session);

        assertEquals(ReviewBoardDecisionType.REQUEST_FURTHER_DISCOVERY, decision.decisionType());
    }

    @Test
    void approveWithConditionsCanRecommendAcceptanceWithConditions() {
        Fixture fixture = new Fixture();
        ReviewBoardSession session = fixture.openWithParticipants();

        session = fixture.workflow.recordVote(session, vote("architect", ReviewBoardVoteType.APPROVE_WITH_CONDITIONS, "Accept if owner is recorded."));
        session = fixture.workflow.recordVote(session, vote("security", ReviewBoardVoteType.APPROVE, "No security objection."));

        ReviewBoardDecision decision = fixture.workflow.deriveReviewBoardDecision(session);

        assertEquals(ReviewBoardDecisionType.ACCEPT_PROPOSED_CHANGE, decision.decisionType());
        assertFalse(decision.conditions().isEmpty());
    }

    private static ReviewBoardVote vote(String participantId, ReviewBoardVoteType type, String rationale) {
        return new ReviewBoardVote(null, participantId, type, rationale, null);
    }

    private static class Fixture {
        final InMemoryAuditSink auditSink = new InMemoryAuditSink();
        final ReviewBoardWorkflowService workflow = new ReviewBoardWorkflowService(auditSink);
        final CorrelationId correlationId = new CorrelationId("review-board-correlation-1");
        final Evidence evidence = new Evidence("evidence-review", "test", "test", 0.9, Instant.now(), List.of("ref"), List.of("artifact"));
        final Observation observation = new Observation("observation-review", "test", "Observation", List.of(evidence), List.of());
        final Finding finding = new Finding("finding-review", Severity.WARNING, "REVIEW", "Finding", List.of(observation), 0.9);
        final Recommendation recommendation = new Recommendation(
                "recommendation-review",
                "Add relationship",
                "Reviewer consensus recommends graph traceability.",
                List.of(),
                List.of(finding),
                "Medium",
                "Low",
                0.9,
                LifecycleStatus.PROPOSED
        );
        final ImmutableKnowledgeGraphAuditLog graphAuditLog = new ImmutableKnowledgeGraphAuditLog();
        final ArchitectureElementService elementService = new ArchitectureElementService(graphAuditLog);
        final RelationshipService relationshipService = new RelationshipService(graphAuditLog);
        final ProposedChangeService proposedChangeService = new ProposedChangeService(elementService, relationshipService);
        final ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("workspace-review");
        final ProposedArchitectureChange proposedChange;

        Fixture() {
            elementService.createElement(graph, new CreateArchitectureElementCommand(
                    ArchitectureElementType.COMPONENT,
                    "Existing Component",
                    "",
                    Map.of(),
                    "architect"
            ));
            proposedChange = proposedChangeService.proposeElementAddition(
                    "workspace-review",
                    correlationId,
                    new ProposedElementAddition(ArchitectureElementType.COMPONENT, "Proposed Component", "", Map.of()),
                    recommendation.id(),
                    List.of(finding.id()),
                    List.of(evidence.id())
            );
        }

        ReviewBoardSession openWithParticipants() {
            ReviewBoardSession session = workflow.openSession(
                    "workspace-review",
                    correlationId,
                    List.of(recommendation),
                    List.of(proposedChange),
                    "lead-architect"
            );
            session = workflow.addParticipant(session, new ReviewBoardParticipant("architect", "Lead Architect", ReviewBoardParticipantType.HUMAN_ARCHITECT));
            session = workflow.addParticipant(session, new ReviewBoardParticipant("security", "Security Reviewer", ReviewBoardParticipantType.SECURITY_REVIEWER));
            return session;
        }
    }
}
