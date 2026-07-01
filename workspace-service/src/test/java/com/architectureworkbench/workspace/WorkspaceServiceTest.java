package com.architectureworkbench.workspace;

import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.CreateArchitectureElementCommand;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceServiceTest {
    @Test
    void createsWorkspaceWithInitialGraphAndAuditEvent() {
        Fixture fixture = new Fixture();

        Workspace workspace = fixture.service.createWorkspace("Mortgage Platform", WorkspaceMetadata.empty(), "architect");

        assertFalse(workspace.id().value().isBlank());
        assertEquals("Mortgage Platform", workspace.name());
        assertEquals(1, fixture.service.listWorkspaces().size());
        assertEquals(workspace.id().value() + "-graph", fixture.service.getWorkspaceGraph(workspace.id()).graphId());
        assertEquals("WorkspaceCreated", fixture.auditSink.entries().get(0).action());
        assertEquals(workspace.id().value(), fixture.auditSink.entries().get(0).architectureEvent().workspaceId());
        assertEquals(MutationTarget.NEITHER, fixture.auditSink.entries().get(0).architectureEvent().mutationTarget());
    }

    @Test
    void renamesWorkspaceAndEmitsAuditEvent() {
        Fixture fixture = new Fixture();
        Workspace workspace = fixture.service.createWorkspace("Mortgage Platform", WorkspaceMetadata.empty(), "architect");

        Workspace renamed = fixture.service.renameWorkspace(workspace.id(), "Mortgage Origination Platform", "architect");

        assertEquals("Mortgage Origination Platform", renamed.name());
        assertEquals("WORKSPACE_RENAMED", fixture.auditSink.entries().get(1).action());
        assertEquals("Mortgage Platform", fixture.auditSink.entries().get(1).details().get("oldName"));
    }

    @Test
    void savesAndRetrievesWorkspaceGraph() {
        Fixture fixture = new Fixture();
        Workspace workspace = fixture.service.createWorkspace("Mortgage Platform", WorkspaceMetadata.empty(), "architect");
        ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("mortgage-kg");
        new ArchitectureElementService(new ImmutableKnowledgeGraphAuditLog()).createElement(graph, new CreateArchitectureElementCommand(
                ArchitectureElementType.BOUNDED_CONTEXT,
                "Mortgage Origination",
                "",
                Map.of(),
                "architect"
        ));

        fixture.service.saveWorkspaceGraph(workspace.id(), graph, "architect");
        ArchitectureKnowledgeGraph loaded = fixture.service.getWorkspaceGraph(workspace.id());

        assertEquals("mortgage-kg", loaded.graphId());
        assertEquals(1, loaded.elements().size());
        assertEquals("WORKSPACE_GRAPH_SAVED", fixture.auditSink.entries().get(1).action());
    }

    @Test
    void importsAndExportsGraphSnapshot() {
        Fixture fixture = new Fixture();
        Workspace workspace = fixture.service.createWorkspace("Imported Codebase", WorkspaceMetadata.empty(), "architect");
        GraphSnapshot snapshot = new GraphSnapshot(
                "imported-kg",
                null,
                java.util.List.of(new GraphSnapshot.ElementSnapshot(
                        "bounded-context-imported",
                        ArchitectureElementType.BOUNDED_CONTEXT,
                        "Imported Context",
                        "Recovered from repository evidence.",
                        Map.of("source", "repo")
                )),
                java.util.List.of()
        );

        ArchitectureKnowledgeGraph imported = fixture.service.importInitialGraphSnapshot(workspace.id(), snapshot, "architect");
        GraphSnapshot exported = fixture.service.exportGraphSnapshot(workspace.id(), "architect");

        assertEquals("imported-kg", imported.graphId());
        assertEquals("imported-kg", exported.graphId());
        assertEquals(1, exported.elements().size());
        assertEquals("bounded-context-imported", exported.elements().get(0).id());
        assertEquals("GraphImported", fixture.auditSink.entries().get(1).action());
        assertEquals("WORKSPACE_GRAPH_EXPORTED", fixture.auditSink.entries().get(2).action());
    }

    @Test
    void emitsAuditEventsForWorkspaceAndGraphOperations() {
        Fixture fixture = new Fixture();
        Workspace workspace = fixture.service.createWorkspace("Mortgage Platform", WorkspaceMetadata.empty(), "architect");
        ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("audit-kg");

        fixture.service.renameWorkspace(workspace.id(), "Mortgage Platform Renamed", "architect");
        fixture.service.saveWorkspaceGraph(workspace.id(), graph, "architect");
        fixture.service.exportGraphSnapshot(workspace.id(), "architect");

        assertEquals(3, fixture.auditSink.entriesForScope("WORKSPACE", workspace.id().value()).size());
        assertEquals(1, fixture.auditSink.entriesForScope("ARCHITECTURE", workspace.id().value()).size());
        assertTrue(fixture.auditSink.entries().stream().anyMatch(event -> event.action().equals("WorkspaceCreated")));
        assertTrue(fixture.auditSink.entries().stream().anyMatch(event -> event.action().equals("WORKSPACE_RENAMED")));
        assertTrue(fixture.auditSink.entries().stream().anyMatch(event -> event.action().equals("WORKSPACE_GRAPH_SAVED")));
        assertTrue(fixture.auditSink.entries().stream().anyMatch(event -> event.action().equals("WORKSPACE_GRAPH_EXPORTED")));
        assertFalse(fixture.auditSink.entries().get(1).previousHash().equals("GENESIS"));
    }

    private static class Fixture {
        final InMemoryAuditSink auditSink = new InMemoryAuditSink();
        final WorkspaceService service = new WorkspaceService(
                new InMemoryWorkspaceRepository(),
                new InMemoryArchitectureGraphRepository(),
                auditSink
        );
    }
}
