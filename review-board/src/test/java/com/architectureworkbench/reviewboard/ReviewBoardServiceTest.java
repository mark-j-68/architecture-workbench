package com.architectureworkbench.reviewboard;

import com.architectureworkbench.agentcollaboration.FindingSeverity;
import com.architectureworkbench.agentcollaboration.ReviewConsensus;
import com.architectureworkbench.agentcollaboration.ReviewFinding;
import com.architectureworkbench.agentcollaboration.ReviewRunResult;
import com.architectureworkbench.agentcollaboration.ReviewerType;
import com.architectureworkbench.intelligence.LifecycleStatus;
import com.architectureworkbench.intelligence.Severity;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.CreateArchitectureElementCommand;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.ProposedChangeStatus;
import com.architectureworkbench.knowledgegraph.ProposedChangeType;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import com.architectureworkbench.knowledgegraph.RelationshipType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewBoardServiceTest {
    @Test
    void normalizesReviewFindingsToAimFindingsAndRecommendationCandidate() {
        ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("kg-review-board-test");
        ImmutableKnowledgeGraphAuditLog auditLog = new ImmutableKnowledgeGraphAuditLog();
        ArchitectureElementService elements = new ArchitectureElementService(auditLog);
        RelationshipService relationships = new RelationshipService(auditLog);
        ReviewBoardService service = new ReviewBoardService(elements, relationships);
        elements.createElement(graph, new CreateArchitectureElementCommand(
                ArchitectureElementType.SYSTEM,
                "Customer Platform",
                "",
                Map.of(),
                "architect"
        ));
        elements.createElement(graph, new CreateArchitectureElementCommand(
                ArchitectureElementType.COMPONENT,
                "Customer API",
                "",
                Map.of(),
                "architect"
        ));
        ReviewFinding finding = new ReviewFinding(
                "security-pii-encryption-missing",
                ReviewerType.SECURITY,
                FindingSeverity.CRITICAL,
                "PII encryption evidence is missing",
                "PII is referenced without encryption evidence.",
                "Add encrypted payload handling.",
                0.9,
                Map.of()
        );
        ReviewConsensus consensus = new ReviewConsensus(
                "review-board-test",
                List.of(finding),
                List.of(),
                0.86,
                "Create remediation decisions for agreed high-severity findings before implementation.",
                Instant.now(),
                "audit-1"
        );

        ReviewBoardRecord record = service.recordReviewRun(graph, new ReviewRunResult("review-board-test", List.of(), consensus), "architect");

        assertEquals(0, graph.elementsOfType(ArchitectureElementType.ARCHITECTURE_REVIEW).size());
        assertEquals(0, graph.elementsOfType(ArchitectureElementType.DECISION).size());
        assertEquals(0, graph.elementsOfType(ArchitectureElementType.RISK).size());
        assertEquals(0, graph.elementsOfType(ArchitectureElementType.EVIDENCE).size());
        assertEquals(1, record.aimFindings().size());
        assertEquals(Severity.CRITICAL, record.aimFindings().get(0).severity());
        assertEquals(1, record.aimFindings().get(0).supportingObservations().size());
        assertEquals(1, record.aimFindings().get(0).supportingObservations().get(0).relatedEvidence().size());
        assertEquals("review-board:SECURITY", record.aimFindings().get(0).supportingObservations().get(0).relatedEvidence().get(0).source());
        assertEquals(LifecycleStatus.PROPOSED, record.recommendationCandidate().lifecycleStatus());
        assertEquals(1, record.recommendationCandidate().supportingFindings().size());
        assertEquals(0, record.riskIds().size());
        assertEquals(1, record.proposedChanges().size());
        assertEquals(ProposedChangeType.RELATIONSHIP_ADDITION, record.proposedChanges().get(0).type());
        assertEquals(ProposedChangeStatus.PROPOSED, record.proposedChanges().get(0).status());
        assertEquals(record.recommendationCandidate().id(), record.proposedChanges().get(0).recommendationId());
        assertTrue(record.proposedChanges().get(0).findingIds().contains(record.aimFindings().get(0).id()));
        assertTrue(record.proposedChanges().get(0).evidenceIds().contains(record.aimFindings().get(0).supportingObservations().get(0).relatedEvidence().get(0).id()));
        assertTrue(graph.relationships().isEmpty());
    }
}
