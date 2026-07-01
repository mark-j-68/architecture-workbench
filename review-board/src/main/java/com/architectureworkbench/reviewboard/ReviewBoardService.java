package com.architectureworkbench.reviewboard;

import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.agentcollaboration.ReviewConsensus;
import com.architectureworkbench.agentcollaboration.ReviewFinding;
import com.architectureworkbench.agentcollaboration.ReviewRunResult;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import java.util.ArrayList;
import java.util.List;

public class ReviewBoardService {
    private final ArchitectureElementService elementService;
    private final RelationshipService relationshipService;
    private final ReviewFindingToAimFindingMapper aimFindingMapper = new ReviewFindingToAimFindingMapper();
    private final ReviewConsensusToRecommendationMapper recommendationMapper = new ReviewConsensusToRecommendationMapper();
    private final ReviewBoardProposedChangeMapper proposedChangeMapper;

    public ReviewBoardService(ArchitectureElementService elementService, RelationshipService relationshipService) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
        this.proposedChangeMapper = new ReviewBoardProposedChangeMapper(new ProposedChangeService(elementService, relationshipService));
    }

    public ReviewBoardRecord recordReviewRun(ArchitectureKnowledgeGraph graph, ReviewRunResult result, String actorRef) {
        ReviewConsensus consensus = result.consensus();
        List<ReviewFinding> reviewFindings = allFindings(consensus);
        List<Finding> aimFindings = reviewFindings.stream()
                .map(finding -> aimFindingMapper.map(result.reviewId(), finding))
                .toList();
        Recommendation recommendationCandidate = aimFindings.isEmpty() ? null : recommendationMapper.map(consensus, aimFindings);
        List<com.architectureworkbench.knowledgegraph.ProposedArchitectureChange> proposedChanges = proposedChangeMapper.proposeRelationshipChanges(
                graph,
                result.reviewId(),
                recommendationCandidate,
                aimFindings,
                new CorrelationId("review-" + result.reviewId())
        );

        return new ReviewBoardRecord(null, null, List.of(), List.of(), aimFindings, recommendationCandidate, proposedChanges);
    }

    private static List<ReviewFinding> allFindings(ReviewConsensus consensus) {
        List<ReviewFinding> findings = new ArrayList<>();
        findings.addAll(consensus.agreedFindings());
        findings.addAll(consensus.conflictingFindings());
        return findings;
    }
}
