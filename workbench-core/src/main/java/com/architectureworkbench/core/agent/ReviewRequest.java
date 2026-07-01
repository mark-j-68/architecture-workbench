package com.architectureworkbench.core.agent;

import com.architectureworkbench.core.model.ArchitectureModel;
import java.util.Objects;

public record ReviewRequest(
        String workspaceId,
        String actorRef,
        ReviewKind kind,
        ArchitectureModel model,
        String question
) {
    public ReviewRequest {
        workspaceId = Objects.requireNonNullElse(workspaceId, "default-workspace");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        kind = Objects.requireNonNullElse(kind, ReviewKind.ARCHITECTURE);
        model = Objects.requireNonNull(model, "model");
        question = Objects.requireNonNullElse(question, "");
    }
}
