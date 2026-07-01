package com.architectureworkbench.workspace;

import com.architectureworkbench.audit.AuditEvent;
import com.architectureworkbench.audit.AuditEventRecord;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.MutationTarget;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWorkspacePersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsWorkspaceAndReloadsAfterRepositoryRestart() {
        WorkspaceService service = service(tempDir);
        Workspace workspace = service.createWorkspace("Persistent Workspace", WorkspaceMetadata.empty(), "architect");

        WorkspaceService restarted = service(tempDir);

        Workspace reloaded = restarted.listWorkspaces().get(0);
        assertEquals(workspace.id(), reloaded.id());
        assertEquals("Persistent Workspace", reloaded.name());
        assertEquals(workspace.id().value() + "-graph", restarted.getWorkspaceGraph(workspace.id()).graphId());
        assertTrue(Files.exists(tempDir.resolve(workspace.id().value()).resolve("manifest.json")));
    }

    @Test
    void savesGraphAndReloadsAfterRepositoryRestart() {
        WorkspaceService service = service(tempDir);
        Workspace workspace = service.createWorkspace("Graph Workspace", WorkspaceMetadata.empty(), "architect");
        ArchitectureKnowledgeGraph graph = service.getWorkspaceGraph(workspace.id());
        new ArchitectureElementService(new ImmutableKnowledgeGraphAuditLog()).createElement(graph, new CreateArchitectureElementCommand(
                ArchitectureElementType.COMPONENT,
                "Customer API",
                "Discovered Spring controller.",
                Map.of("source", "test"),
                "architect"
        ));

        service.saveWorkspaceGraph(workspace.id(), graph, "architect");
        WorkspaceService restarted = service(tempDir);

        ArchitectureKnowledgeGraph reloaded = restarted.getWorkspaceGraph(workspace.id());
        assertEquals(1, reloaded.elements().size());
        assertEquals("Customer API", reloaded.elements().iterator().next().name());

        WorkspaceIntegrityReport report = new FileWorkspaceIntegrityService(tempDir).verifyWorkspace(workspace.id());
        assertTrue(report.valid(), () -> String.join(", ", report.failures()));
    }

    @Test
    void persistsAcceptedProposedChangeResult() {
        WorkspaceService service = service(tempDir);
        Workspace workspace = service.createWorkspace("Proposal Workspace", WorkspaceMetadata.empty(), "architect");
        ArchitectureKnowledgeGraph graph = service.getWorkspaceGraph(workspace.id());
        ImmutableKnowledgeGraphAuditLog graphAudit = new ImmutableKnowledgeGraphAuditLog();
        ProposedChangeService proposedChangeService = new ProposedChangeService(
                new ArchitectureElementService(graphAudit),
                new RelationshipService(graphAudit)
        );
        ProposedChangeRepository repository = new FileProposedChangeRepository(tempDir);
        ProposedArchitectureChange proposed = proposedChangeService.proposeElementAddition(
                graph.graphId(),
                new CorrelationId("proposal-correlation"),
                new ProposedElementAddition(ArchitectureElementType.COMPONENT, "Recovered Component", "", Map.of()),
                "recommendation-1",
                List.of("finding-1"),
                List.of("evidence-1")
        );

        ProposedArchitectureChange accepted = proposedChangeService.acceptProposedChange(graph, proposed, "architect", "Accepted for test.");
        repository.save(accepted);

        ProposedChangeRepository restarted = new FileProposedChangeRepository(tempDir);
        ProposedArchitectureChange reloaded = restarted.findById(accepted.id().value()).orElseThrow();
        assertEquals(ProposedChangeStatus.ACCEPTED, reloaded.status());
        assertEquals("Accepted for test.", reloaded.decisionRationale());
    }

    @Test
    void retainsAuditEventsAcrossSinkRestart() {
        FileAuditSink auditSink = new FileAuditSink(tempDir);
        WorkspaceService service = new WorkspaceService(
                new FileWorkspaceRepository(tempDir),
                new FileArchitectureGraphRepository(tempDir),
                auditSink
        );
        Workspace workspace = service.createWorkspace("Audited Workspace", WorkspaceMetadata.empty(), "architect");

        FileAuditSink restarted = new FileAuditSink(tempDir);
        List<AuditEvent> events = restarted.entriesForScope("ARCHITECTURE", workspace.id().value());

        assertEquals(1, events.size());
        assertEquals("WorkspaceCreated", events.get(0).action());
        assertEquals(MutationTarget.NEITHER, events.get(0).architectureEvent().mutationTarget());
        assertFalse(events.get(0).eventHash().isBlank());
    }

    @Test
    void corruptWorkspaceFileFailsWithUsefulError() throws IOException {
        Path workspaceDirectory = tempDir.resolve("workspace-corrupt");
        Files.createDirectories(workspaceDirectory);
        Files.writeString(workspaceDirectory.resolve("workspace.json"), "{not-json");

        FileWorkspaceRepository repository = new FileWorkspaceRepository(tempDir);

        IllegalStateException exception = assertThrows(IllegalStateException.class, repository::findAll);
        assertTrue(exception.getMessage().contains("Unable to read JSON file"));
    }

    @Test
    void checksumFailureIsDetected() throws IOException {
        WorkspaceService service = service(tempDir);
        Workspace workspace = service.createWorkspace("Checksum Workspace", WorkspaceMetadata.empty(), "architect");
        service.saveWorkspaceGraph(workspace.id(), service.getWorkspaceGraph(workspace.id()), "architect");

        Files.writeString(tempDir.resolve(workspace.id().value()).resolve("graph.json"), "{}");

        WorkspaceIntegrityReport report = new FileWorkspaceIntegrityService(tempDir).verifyWorkspace(workspace.id());
        assertFalse(report.valid());
        assertTrue(report.failures().stream().anyMatch(failure -> failure.contains("Checksum mismatch for file: graph.json")));
    }

    @Test
    void corruptCurrentFileRecoversFromValidBackup() throws IOException {
        FileWorkspaceRepository repository = new FileWorkspaceRepository(tempDir);
        Workspace workspace = repository.save(new Workspace(WorkspaceId.newId(), "Initial Name", WorkspaceMetadata.empty()));
        repository.save(workspace.rename("Updated Name"));
        Files.writeString(tempDir.resolve(workspace.id().value()).resolve("workspace.json"), "{not-json");

        Workspace recovered = repository.findById(workspace.id()).orElseThrow();

        assertEquals("Initial Name", recovered.name());
        assertTrue(Files.readString(tempDir.resolve(workspace.id().value()).resolve("workspace.json")).contains("Initial Name"));
    }

    @Test
    void corruptCurrentAndBackupFailsSafely() throws IOException {
        FileWorkspaceRepository repository = new FileWorkspaceRepository(tempDir);
        Workspace workspace = repository.save(new Workspace(WorkspaceId.newId(), "Initial Name", WorkspaceMetadata.empty()));
        repository.save(workspace.rename("Updated Name"));
        Path workspaceFile = tempDir.resolve(workspace.id().value()).resolve("workspace.json");
        Files.writeString(workspaceFile, "{not-json");
        Files.writeString(workspaceFile.resolveSibling("workspace.json.bak"), "{also-not-json");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> repository.findById(workspace.id()));

        assertTrue(exception.getMessage().contains("backup is invalid"));
    }

    @Test
    void auditHashChainBreakIsDetected() {
        WorkspaceService service = service(tempDir);
        Workspace workspace = service.createWorkspace("Audit Chain Workspace", WorkspaceMetadata.empty(), "architect");
        service.saveWorkspaceGraph(workspace.id(), service.getWorkspaceGraph(workspace.id()), "architect");
        Path auditFile = tempDir.resolve(workspace.id().value()).resolve("audit-events.json");
        List<AuditEventRecord> records = WorkspaceJson.read(auditFile, new TypeReference<>() {});
        AuditEventRecord broken = new AuditEventRecord(
                records.get(1).eventId(),
                records.get(1).occurredAt(),
                records.get(1).architectureEvent(),
                records.get(1).scopeType(),
                records.get(1).scopeId(),
                records.get(1).actorRef(),
                records.get(1).action(),
                records.get(1).subjectRef(),
                records.get(1).details(),
                "broken-previous-hash",
                records.get(1).eventHash()
        );
        WorkspaceJson.write(auditFile, List.of(records.get(0), broken));

        WorkspaceIntegrityReport report = new FileWorkspaceIntegrityService(tempDir).verifyWorkspace(workspace.id());

        assertFalse(report.valid());
        assertTrue(report.failures().stream().anyMatch(failure -> failure.contains("Audit hash chain break")));
    }

    private static WorkspaceService service(Path root) {
        return new WorkspaceService(
                new FileWorkspaceRepository(root),
                new FileArchitectureGraphRepository(root),
                new FileAuditSink(root)
        );
    }
}
