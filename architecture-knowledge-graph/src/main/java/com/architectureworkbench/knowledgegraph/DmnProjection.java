package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record DmnProjection(
        List<String> decisions,
        List<String> policies,
        List<String> risks
) implements ProjectionPayload {
    public DmnProjection {
        decisions = List.copyOf(decisions == null ? List.of() : decisions);
        policies = List.copyOf(policies == null ? List.of() : policies);
        risks = List.copyOf(risks == null ? List.of() : risks);
    }
}
