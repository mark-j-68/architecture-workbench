package com.architectureworkbench.workspace;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.GraphImported;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.audit.WorkspaceCreated;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import java.util.List;
import java.util.Map;

public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final ArchitectureGraphRepository graphRepository;
    private final AuditSink auditSink;
    private final GraphSnapshotMapper snapshotMapper = new GraphSnapshotMapper();

    public WorkspaceService(WorkspaceRepository workspaceRepository, ArchitectureGraphRepository graphRepository, AuditSink auditSink) {
        this.workspaceRepository = workspaceRepository;
        this.graphRepository = graphRepository;
        this.auditSink = auditSink;
    }

    public Workspace createWorkspace(String name, WorkspaceMetadata metadata, String actorRef) {
        Workspace workspace = workspaceRepository.save(new Workspace(WorkspaceId.newId(), name, metadata));
        graphRepository.save(workspace.id(), new ArchitectureKnowledgeGraph(workspace.id().value() + "-graph"));
        auditWorkspaceCreated(workspace, actorRef);
        return workspace;
    }

    public Workspace renameWorkspace(WorkspaceId workspaceId, String newName, String actorRef) {
        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace does not exist: " + workspaceId.value()));
        Workspace renamed = workspaceRepository.save(existing.rename(newName));
        audit("WORKSPACE_RENAMED", workspaceId, workspaceId.value(), actorRef, Map.of("oldName", existing.name(), "newName", renamed.name()));
        return renamed;
    }

    public List<Workspace> listWorkspaces() {
        return workspaceRepository.findAll();
    }

    public ArchitectureKnowledgeGraph getWorkspaceGraph(WorkspaceId workspaceId) {
        requireWorkspace(workspaceId);
        return graphRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new IllegalStateException("Workspace graph does not exist: " + workspaceId.value()));
    }

    public ArchitectureKnowledgeGraph saveWorkspaceGraph(WorkspaceId workspaceId, ArchitectureKnowledgeGraph graph, String actorRef) {
        requireWorkspace(workspaceId);
        ArchitectureKnowledgeGraph saved = graphRepository.save(workspaceId, graph);
        audit("WORKSPACE_GRAPH_SAVED", workspaceId, graph.graphId(), actorRef, Map.of("graphId", graph.graphId()));
        return saved;
    }

    public ArchitectureKnowledgeGraph importInitialGraphSnapshot(WorkspaceId workspaceId, GraphSnapshot snapshot, String actorRef) {
        requireWorkspace(workspaceId);
        ArchitectureKnowledgeGraph graph = snapshotMapper.importSnapshot(snapshot);
        graphRepository.save(workspaceId, graph);
        auditGraphImported(workspaceId, graph, snapshot, actorRef);
        return graph;
    }

    public GraphSnapshot exportGraphSnapshot(WorkspaceId workspaceId, String actorRef) {
        ArchitectureKnowledgeGraph graph = getWorkspaceGraph(workspaceId);
        GraphSnapshot snapshot = snapshotMapper.exportSnapshot(graph);
        audit("WORKSPACE_GRAPH_EXPORTED", workspaceId, graph.graphId(), actorRef, Map.of(
                "graphId", graph.graphId(),
                "elementCount", String.valueOf(snapshot.elements().size()),
                "relationshipCount", String.valueOf(snapshot.relationships().size())
        ));
        return snapshot;
    }

    private void requireWorkspace(WorkspaceId workspaceId) {
        if (workspaceRepository.findById(workspaceId).isEmpty()) {
            throw new IllegalArgumentException("Workspace does not exist: " + workspaceId.value());
        }
    }

    private void audit(String action, WorkspaceId workspaceId, String subjectRef, String actorRef, Map<String, String> details) {
        auditSink.append(new com.architectureworkbench.audit.AuditAppendRequest("WORKSPACE", workspaceId.value(), actorRef, action, subjectRef, details));
    }

    private void auditWorkspaceCreated(Workspace workspace, String actorRef) {
        WorkspaceCreated event = new WorkspaceCreated(workspace.id().value(), workspace.name());
        auditSink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.WORKSPACE_CREATED,
                workspace.id().value(),
                ArchitectureEventSource.WORKSPACE_SERVICE,
                Actor.human(actorRef),
                CausationId.newId("create-workspace"),
                CorrelationId.newId("workspace"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                event.payload(),
                null
        ));
    }

    private void auditGraphImported(WorkspaceId workspaceId, ArchitectureKnowledgeGraph graph, GraphSnapshot snapshot, String actorRef) {
        GraphImported event = new GraphImported(graph.graphId(), snapshot.elements().size(), snapshot.relationships().size());
        auditSink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.GRAPH_IMPORTED,
                workspaceId.value(),
                ArchitectureEventSource.WORKSPACE_SERVICE,
                Actor.human(actorRef),
                CausationId.newId("import-graph"),
                CorrelationId.newId("workspace"),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.GRAPH,
                event.payload(),
                null
        ));
    }
}
