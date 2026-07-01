package com.architectureworkbench.reviewboard;

import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.ProposedRelationshipAddition;
import com.architectureworkbench.knowledgegraph.RelationshipType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviewBoardProposedChangeMapper {
    private final ProposedChangeService proposedChangeService;

    public ReviewBoardProposedChangeMapper(ProposedChangeService proposedChangeService) {
        this.proposedChangeService = proposedChangeService;
    }

    public List<ProposedArchitectureChange> proposeRelationshipChanges(
            ArchitectureKnowledgeGraph graph,
            String reviewId,
            Recommendation recommendation,
            List<Finding> findings,
            CorrelationId correlationId
    ) {
        if (recommendation == null || findings == null || findings.isEmpty()) {
            return List.of();
        }
        List<ArchitectureElement> elements = new ArrayList<>(graph.elements());
        if (elements.size() < 2) {
            return List.of();
        }
        ArchitectureElement source = elements.get(0);
        ArchitectureElement target = elements.get(1);
        return List.of(proposedChangeService.proposeRelationshipAddition(
                graph.graphId(),
                correlationId,
                new ProposedRelationshipAddition(
                        source.id(),
                        target.id(),
                        RelationshipType.TRACES_TO,
                        "review recommendation trace",
                        Map.of("reviewId", reviewId, "source", "review-board")
                ),
                recommendation.id(),
                findings.stream().map(Finding::id).toList(),
                findings.stream()
                        .flatMap(finding -> finding.supportingObservations().stream())
                        .flatMap(observation -> observation.relatedEvidence().stream())
                        .map(com.architectureworkbench.intelligence.Evidence::id)
                        .distinct()
                        .toList()
        ));
    }
}
