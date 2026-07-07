package com.architectureworkbench.discovery;

import java.nio.file.Path;
import java.util.Objects;

public record DiscoveryExecutionContext(
        DiscoveryRunId runId,
        DiscoverySource source,
        Path rootDirectory,
        String actorRef,
        String correlationId
) {
    public DiscoveryExecutionContext {
        runId = Objects.requireNonNull(runId, "runId");
        source = Objects.requireNonNull(source, "source");
        rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        correlationId = Objects.requireNonNullElse(correlationId, runId.value());
    }

    public static DiscoveryExecutionContext from(DiscoveryContext context) {
        return new DiscoveryExecutionContext(
                context.runId(),
                context.source(),
                context.rootDirectory(),
                context.actorRef(),
                context.runId().value()
        );
    }
}
