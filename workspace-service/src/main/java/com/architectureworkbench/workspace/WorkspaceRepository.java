package com.architectureworkbench.workspace;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {
    Workspace save(Workspace workspace);
    Optional<Workspace> findById(WorkspaceId workspaceId);
    List<Workspace> findAll();
}
