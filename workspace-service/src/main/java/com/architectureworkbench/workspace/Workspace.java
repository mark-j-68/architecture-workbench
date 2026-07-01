package com.architectureworkbench.workspace;

import java.util.Objects;

public record Workspace(
        WorkspaceId id,
        String name,
        WorkspaceMetadata metadata
) {
    public Workspace {
        id = Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workspace name is required.");
        }
        metadata = Objects.requireNonNullElseGet(metadata, WorkspaceMetadata::empty);
    }

    public Workspace rename(String newName) {
        return new Workspace(id, newName, metadata.renamedAt(java.time.Instant.now()));
    }
}
