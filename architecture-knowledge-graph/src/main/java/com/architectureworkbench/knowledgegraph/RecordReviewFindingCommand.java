package com.architectureworkbench.knowledgegraph;

import java.util.Map;
import java.util.Objects;

public record RecordReviewFindingCommand(
        ElementId reviewedElementId,
        String reviewer,
        String finding,
        String recommendation,
        String actorRef
) {
    public RecordReviewFindingCommand {
        reviewedElementId = Objects.requireNonNull(reviewedElementId, "reviewedElementId");
        reviewer = ArchitectureElement.requireText(reviewer, "reviewer");
        finding = ArchitectureElement.requireText(finding, "finding");
        recommendation = Objects.requireNonNullElse(recommendation, "");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
    }

    Map<String, String> attributes() {
        return Map.of("reviewer", reviewer, "finding", finding, "recommendation", recommendation);
    }
}
