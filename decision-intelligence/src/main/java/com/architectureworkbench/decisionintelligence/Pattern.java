package com.architectureworkbench.decisionintelligence;

import java.util.List;

public record Pattern(
        String id,
        String name,
        String description,
        String applicability,
        List<String> knownTradeoffs,
        List<Learning> supportingLearnings
) {
    public Pattern {
        id = DecisionIntelligenceIds.id("pattern", id);
        name = DecisionIntelligenceIds.required(name, "name");
        description = DecisionIntelligenceIds.required(description, "description");
        applicability = DecisionIntelligenceIds.required(applicability, "applicability");
        knownTradeoffs = List.copyOf(knownTradeoffs == null ? List.of() : knownTradeoffs);
        supportingLearnings = List.copyOf(DecisionIntelligenceIds.requireNonEmpty(supportingLearnings == null ? List.of() : supportingLearnings, "supportingLearnings"));
    }
}
