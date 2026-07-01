package com.architectureworkbench.workspace;

import java.util.Objects;
import java.util.UUID;

public record WorkspaceId(String value) {
    public WorkspaceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Workspace id is required.");
        }
    }

    public static WorkspaceId newId() {
        return new WorkspaceId("workspace-" + UUID.randomUUID());
    }

    public static WorkspaceId of(String value) {
        return new WorkspaceId(Objects.requireNonNull(value));
    }
}
