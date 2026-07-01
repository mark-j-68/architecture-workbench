package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record ReviewBoardProjection(
        List<String> reviews,
        List<String> risks,
        List<String> decisions,
        List<String> evidence
) implements ProjectionPayload {
    public ReviewBoardProjection {
        reviews = List.copyOf(reviews == null ? List.of() : reviews);
        risks = List.copyOf(risks == null ? List.of() : risks);
        decisions = List.copyOf(decisions == null ? List.of() : decisions);
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
}
