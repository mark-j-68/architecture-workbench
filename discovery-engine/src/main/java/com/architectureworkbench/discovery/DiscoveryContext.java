package com.architectureworkbench.discovery;

import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import java.nio.file.Path;
import java.util.Objects;

public record DiscoveryContext(
        DiscoveryRunId runId,
        DiscoverySource source,
        Path rootDirectory,
        ArchitectureKnowledgeGraph graph,
        String actorRef
) {
    public DiscoveryContext {
        runId = Objects.requireNonNullElseGet(runId, DiscoveryRunId::newId);
        source = Objects.requireNonNull(source, "source");
        rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        graph = Objects.requireNonNull(graph, "graph");
        actorRef = Objects.requireNonNullElse(actorRef, "system");
    }
}
