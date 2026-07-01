package com.architectureworkbench.workspace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryWorkspaceRepository implements WorkspaceRepository {
    private final Map<WorkspaceId, Workspace> workspaces = new LinkedHashMap<>();

    @Override
    public synchronized Workspace save(Workspace workspace) {
        workspaces.put(workspace.id(), workspace);
        return workspace;
    }

    @Override
    public synchronized Optional<Workspace> findById(WorkspaceId workspaceId) {
        return Optional.ofNullable(workspaces.get(workspaceId));
    }

    @Override
    public synchronized List<Workspace> findAll() {
        return List.copyOf(workspaces.values());
    }
}
