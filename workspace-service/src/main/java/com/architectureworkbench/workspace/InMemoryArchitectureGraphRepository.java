package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryArchitectureGraphRepository implements ArchitectureGraphRepository {
    private final Map<WorkspaceId, ArchitectureKnowledgeGraph> graphs = new LinkedHashMap<>();

    @Override
    public synchronized ArchitectureKnowledgeGraph save(WorkspaceId workspaceId, ArchitectureKnowledgeGraph graph) {
        graphs.put(workspaceId, graph);
        return graph;
    }

    @Override
    public synchronized Optional<ArchitectureKnowledgeGraph> findByWorkspaceId(WorkspaceId workspaceId) {
        return Optional.ofNullable(graphs.get(workspaceId));
    }
}
