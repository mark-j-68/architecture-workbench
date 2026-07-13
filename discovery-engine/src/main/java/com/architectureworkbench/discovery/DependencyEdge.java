package com.architectureworkbench.discovery;

import java.util.List;
import java.util.Objects;

public record DependencyEdge(DependencyNode from, DependencyNode to, String kind, List<String> evidenceIds) {
    public DependencyEdge {
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        kind = Objects.requireNonNullElse(kind, "depends-on");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }
    public String sequence() { return from.id() + " -> " + to.id(); }
}
