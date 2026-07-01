package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryProposedChangeRepository implements ProposedChangeRepository {
    private final Map<String, ProposedArchitectureChange> changes = new LinkedHashMap<>();

    @Override
    public synchronized ProposedArchitectureChange save(ProposedArchitectureChange change) {
        changes.put(change.id().value(), change);
        return change;
    }

    @Override
    public synchronized Optional<ProposedArchitectureChange> findById(String proposedChangeId) {
        return Optional.ofNullable(changes.get(proposedChangeId));
    }

    @Override
    public synchronized List<ProposedArchitectureChange> findByWorkspaceId(String workspaceId) {
        return changes.values().stream()
                .filter(change -> change.workspaceId().equals(workspaceId))
                .toList();
    }
}
