package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import java.util.List;

public record DiscoveryProposalResult(
        List<Recommendation> recommendations,
        List<ProposedArchitectureChange> proposedChanges
) {
    public DiscoveryProposalResult {
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
        proposedChanges = List.copyOf(proposedChanges == null ? List.of() : proposedChanges);
    }
}
