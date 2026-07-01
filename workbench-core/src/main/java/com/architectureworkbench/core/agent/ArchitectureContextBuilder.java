package com.architectureworkbench.core.agent;

import com.architectureworkbench.core.model.ArchitectureModel;

public class ArchitectureContextBuilder {
    public ArchitectureContext build(String workspaceId, ArchitectureModel model) {
        var boundedContexts = model.getDomain().getBoundedContexts();
        int aggregateCount = boundedContexts.stream().mapToInt(ctx -> ctx.getAggregates().size()).sum();
        int commandCount = boundedContexts.stream().mapToInt(ctx -> ctx.getCommands().size()).sum();
        int eventCount = boundedContexts.stream().mapToInt(ctx -> ctx.getEvents().size()).sum();

        return new ArchitectureContext(
                workspaceId,
                boundedContexts.size(),
                aggregateCount,
                commandCount,
                eventCount,
                model.getArchitecture().getServices().size(),
                model.getArchitecture().getIntegrations().size(),
                model.getGovernance().getAudit().isImmutableActivityLogRequired(),
                model.getGovernance().getPiiProtection().isEncryptionRequired(),
                model.getGovernance().getPiiProtection().isCryptographicShreddingRequired(),
                model.getTags()
        );
    }
}
