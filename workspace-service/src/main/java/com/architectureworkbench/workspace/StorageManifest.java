package com.architectureworkbench.workspace;

import java.time.Instant;
import java.util.List;

public record StorageManifest(
        String workspaceId,
        int schemaVersion,
        Instant createdAt,
        Instant updatedAt,
        List<StorageFileChecksum> checksums,
        String lastKnownAuditHash
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public StorageManifest {
        checksums = List.copyOf(checksums == null ? List.of() : checksums);
        lastKnownAuditHash = lastKnownAuditHash == null ? "" : lastKnownAuditHash;
    }
}
