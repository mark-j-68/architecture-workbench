package com.architectureworkbench.agentcollaboration;

import java.util.List;
import java.util.Objects;

public record ReviewRunResult(
        String reviewId,
        List<ReviewResponse> responses,
        ReviewConsensus consensus
) {
    public ReviewRunResult {
        reviewId = ReviewRequest.requireText(reviewId, "reviewId");
        responses = List.copyOf(Objects.requireNonNullElseGet(responses, List::of));
        consensus = Objects.requireNonNull(consensus, "consensus");
    }
}
