package com.architectureworkbench.reviewboard;

import com.architectureworkbench.agentcollaboration.ReviewConsensus;
import com.architectureworkbench.intelligence.LifecycleStatus;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.intelligence.Finding;
import java.util.List;

public class ReviewConsensusToRecommendationMapper {
    public Recommendation map(ReviewConsensus consensus, List<Finding> supportingFindings) {
        if (supportingFindings == null || supportingFindings.isEmpty()) {
            throw new IllegalArgumentException("Consensus recommendation requires AIM findings.");
        }
        return new Recommendation(
                "recommendation-review-" + consensus.reviewId(),
                consensus.recommendedNextAction(),
                "Generated from provider-neutral review consensus. This is a recommendation candidate, not a decision.",
                List.of(),
                supportingFindings,
                "Architecture governance impact depends on accepted remediation scope.",
                "Requires review-board sign-off before implementation.",
                consensus.confidenceScore(),
                LifecycleStatus.PROPOSED
        );
    }
}
