package com.architectureworkbench.reviewboard;

import com.architectureworkbench.knowledgegraph.ElementId;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Recommendation;
import java.util.List;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;

public record ReviewBoardRecord(
        ElementId architectureReviewId,
        ElementId decisionId,
        List<ElementId> riskIds,
        List<ElementId> evidenceIds,
        List<Finding> aimFindings,
        Recommendation recommendationCandidate,
        List<ProposedArchitectureChange> proposedChanges
) {
    public ReviewBoardRecord(ElementId architectureReviewId, ElementId decisionId, List<ElementId> riskIds, List<ElementId> evidenceIds) {
        this(architectureReviewId, decisionId, riskIds, evidenceIds, List.of(), null, List.of());
    }

    public ReviewBoardRecord {
        riskIds = List.copyOf(riskIds == null ? List.of() : riskIds);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        aimFindings = List.copyOf(aimFindings == null ? List.of() : aimFindings);
        proposedChanges = List.copyOf(proposedChanges == null ? List.of() : proposedChanges);
    }
}
