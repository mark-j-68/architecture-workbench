package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record AdrProjection(
        List<AdrSummary> adrs,
        List<String> decisions
) implements ProjectionPayload {
    public AdrProjection {
        adrs = List.copyOf(adrs == null ? List.of() : adrs);
        decisions = List.copyOf(decisions == null ? List.of() : decisions);
    }

    public record AdrSummary(String id, String title, String status) {}
}
