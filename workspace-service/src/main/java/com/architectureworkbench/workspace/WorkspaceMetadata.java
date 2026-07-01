package com.architectureworkbench.workspace;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record WorkspaceMetadata(
        String description,
        String owner,
        String purpose,
        Map<String, String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public WorkspaceMetadata {
        description = Objects.requireNonNullElse(description, "");
        owner = Objects.requireNonNullElse(owner, "");
        purpose = Objects.requireNonNullElse(purpose, "");
        tags = Map.copyOf(Objects.requireNonNullElseGet(tags, Map::of));
        createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        updatedAt = Objects.requireNonNullElse(updatedAt, createdAt);
    }

    public static WorkspaceMetadata empty() {
        Instant now = Instant.now();
        return new WorkspaceMetadata("", "", "", Map.of(), now, now);
    }

    public WorkspaceMetadata renamedAt(Instant updatedAt) {
        return new WorkspaceMetadata(description, owner, purpose, tags, createdAt, updatedAt);
    }
}
