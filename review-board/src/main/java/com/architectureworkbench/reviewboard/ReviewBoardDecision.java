package com.architectureworkbench.reviewboard;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ReviewBoardDecision(
        ReviewBoardDecisionType decisionType,
        String rationale,
        List<String> conditions,
        Instant decidedAt
) {
    public ReviewBoardDecision {
        decisionType = Objects.requireNonNull(decisionType, "decisionType");
        rationale = Objects.requireNonNullElse(rationale, "");
        conditions = List.copyOf(conditions == null ? List.of() : conditions);
        decidedAt = Objects.requireNonNullElseGet(decidedAt, Instant::now);
    }
}
