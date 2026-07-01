package com.architectureworkbench.agentcollaboration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ReviewRequest(
        String reviewId,
        String workspaceId,
        String actorRef,
        String architectureContext,
        List<String> focusAreas,
        Map<String, String> metadata
) {
    public ReviewRequest {
        reviewId = Objects.requireNonNullElseGet(reviewId, () -> "review-" + UUID.randomUUID());
        workspaceId = requireText(Objects.requireNonNullElse(workspaceId, "default-workspace"), "workspaceId");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        architectureContext = Objects.requireNonNullElse(architectureContext, "");
        focusAreas = List.copyOf(Objects.requireNonNullElseGet(focusAreas, List::of));
        metadata = Map.copyOf(Objects.requireNonNullElseGet(metadata, Map::of));
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
