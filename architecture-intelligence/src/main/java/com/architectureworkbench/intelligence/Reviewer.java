package com.architectureworkbench.intelligence;

import java.util.List;
import java.util.Optional;

public record Reviewer(
        String id,
        ReviewerType reviewerType,
        List<String> capabilities,
        Optional<String> provider,
        String version,
        HumanOrAutomated humanOrAutomated
) {
    public Reviewer {
        id = AimIds.id("reviewer", id);
        reviewerType = java.util.Objects.requireNonNull(reviewerType, "reviewerType");
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        provider = provider == null ? Optional.empty() : provider;
        version = java.util.Objects.requireNonNullElse(version, "");
        humanOrAutomated = java.util.Objects.requireNonNull(humanOrAutomated, "humanOrAutomated");
    }
}
