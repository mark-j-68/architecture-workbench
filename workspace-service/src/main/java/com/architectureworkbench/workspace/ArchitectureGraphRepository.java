package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import java.util.Optional;

public interface ArchitectureGraphRepository {
    ArchitectureKnowledgeGraph save(WorkspaceId workspaceId, ArchitectureKnowledgeGraph graph);
    Optional<ArchitectureKnowledgeGraph> findByWorkspaceId(WorkspaceId workspaceId);
}
