package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import java.util.List;
import java.util.Optional;

public interface ProposedChangeRepository {
    ProposedArchitectureChange save(ProposedArchitectureChange change);
    Optional<ProposedArchitectureChange> findById(String proposedChangeId);
    List<ProposedArchitectureChange> findByWorkspaceId(String workspaceId);
}
