package com.architectureworkbench.core.agent;

import java.util.List;

public record ArchitectureContext(
        String workspaceId,
        int boundedContextCount,
        int aggregateCount,
        int commandCount,
        int eventCount,
        int serviceCount,
        int integrationCount,
        boolean immutableAuditRequired,
        boolean piiEncrypted,
        boolean cryptoShreddingRequired,
        List<String> tags
) {}
