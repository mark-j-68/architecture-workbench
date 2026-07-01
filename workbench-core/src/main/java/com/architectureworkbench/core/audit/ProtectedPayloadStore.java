package com.architectureworkbench.core.audit;

import com.architectureworkbench.core.model.audit.ProtectedPayloadReference;

public interface ProtectedPayloadStore {
    ProtectedPayloadReference store(String workspaceId, String correlationId, String classification, String payload);
    String retrieve(String payloadId);
    boolean shred(String payloadId);
}
