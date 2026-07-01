package com.architectureworkbench.audit;

import java.util.Map;
import java.util.Objects;

public record WorkspaceCreated(String workspaceId, String name) {
    public WorkspaceCreated {
        workspaceId = AuditEventRecord.required(workspaceId, "workspaceId");
        name = AuditEventRecord.required(name, "name");
    }

    public Map<String, String> payload() {
        return Map.of("workspaceId", workspaceId, "name", name);
    }
}
