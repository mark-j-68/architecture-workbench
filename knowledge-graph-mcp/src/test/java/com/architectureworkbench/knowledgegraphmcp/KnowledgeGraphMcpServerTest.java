package com.architectureworkbench.knowledgegraphmcp;

import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ArchitectureReview;
import com.architectureworkbench.knowledgegraph.CreateArchitectureElementCommand;
import com.architectureworkbench.knowledgegraph.DddConsistencyValidationService;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.Projection;
import com.architectureworkbench.knowledgegraph.ProjectionService;
import com.architectureworkbench.knowledgegraph.ProjectionType;
import com.architectureworkbench.knowledgegraph.ReactFlowProjection;
import com.architectureworkbench.knowledgegraph.ValidationReport;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeGraphMcpServerTest {
    @Test
    void exposesGraphCentredReadValidateProjectionAndReviewProposalTools() {
        Fixture fixture = new Fixture();
        ArchitectureElement aggregate = fixture.elements.createElement(fixture.graph, new CreateArchitectureElementCommand(
                ArchitectureElementType.AGGREGATE,
                "Application",
                "",
                Map.of(),
                "architect"
        ));

        GraphSnapshot snapshot = (GraphSnapshot) fixture.server.callTool(new KnowledgeGraphMcpToolCall("read_graph", Map.of("graph", fixture.graph)));
        ArchitectureElement found = (ArchitectureElement) fixture.server.callTool(new KnowledgeGraphMcpToolCall("find_element", Map.of("graph", fixture.graph, "elementId", aggregate.id().value())));
        ValidationReport report = (ValidationReport) fixture.server.callTool(new KnowledgeGraphMcpToolCall("validate_graph", Map.of("graph", fixture.graph)));
        Projection projection = (Projection) fixture.server.callTool(new KnowledgeGraphMcpToolCall("generate_projection", Map.of("graph", fixture.graph, "projectionType", ProjectionType.REACT_FLOW.name(), "actorRef", "architect")));
        ArchitectureReview proposal = (ArchitectureReview) fixture.server.callTool(new KnowledgeGraphMcpToolCall("create_review_proposal", Map.of(
                "graph", fixture.graph,
                "title", "Review proposed aggregate boundary",
                "finding", "Aggregate needs root entity evidence.",
                "recommendation", "Add root entity before implementation.",
                "actorRef", "architect"
        )));

        assertEquals(1, snapshot.elements().size());
        assertEquals(aggregate.id(), found.id());
        assertTrue(report.hasErrors());
        assertInstanceOf(ReactFlowProjection.class, projection.payload());
        assertEquals(ArchitectureElementType.ARCHITECTURE_REVIEW, proposal.type());
        assertTrue(fixture.auditLog.entriesForGraph("kg-mcp-test").stream().anyMatch(event -> event.action().equals("ElementAdded")));
    }

    @Test
    void rejectsUncontrolledGraphMutationTools() {
        Fixture fixture = new Fixture();

        assertThrows(IllegalArgumentException.class, () -> fixture.server.callTool(new KnowledgeGraphMcpToolCall("apply_graph_patch", Map.of("graph", fixture.graph))));
        assertTrue(fixture.server.listTools().stream().noneMatch(tool -> tool.name().equals("apply_graph_patch")));
    }

    private static class Fixture {
        final ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("kg-mcp-test");
        final ImmutableKnowledgeGraphAuditLog auditLog = new ImmutableKnowledgeGraphAuditLog();
        final ArchitectureElementService elements = new ArchitectureElementService(auditLog);
        final KnowledgeGraphMcpServer server = new KnowledgeGraphMcpServer(
                elements,
                new ProjectionService(auditLog),
                new DddConsistencyValidationService()
        );
    }
}
