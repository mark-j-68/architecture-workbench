package com.architectureworkbench.workspace;

import java.util.List;

public record WorkspaceIntegrityReport(boolean valid, List<String> failures, String lastKnownAuditHash) {
    public WorkspaceIntegrityReport {
        failures = List.copyOf(failures == null ? List.of() : failures);
        lastKnownAuditHash = lastKnownAuditHash == null ? "" : lastKnownAuditHash;
    }
}
