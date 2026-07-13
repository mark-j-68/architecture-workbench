package com.architectureworkbench.discovery;

import java.util.List;
import java.util.Objects;

public record TopologyEdge(TopologyNode from, TopologyNode to, String relationship, List<String> evidenceIds) {
    public TopologyEdge {
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        relationship = Objects.requireNonNullElse(relationship, "connects");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }
}
