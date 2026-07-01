package com.architectureworkbench.intelligence;

import java.time.Instant;
import java.util.List;

public class DecisionService {
    public DecisionOutcome recordDecision(
            DecisionStatus status,
            String rationale,
            List<Reviewer> reviewers,
            List<Evidence> evidence,
            List<Recommendation> recommendations
    ) {
        return new DecisionOutcome(
                null,
                status,
                rationale,
                reviewers,
                evidence,
                recommendations,
                Instant.now()
        );
    }
}
