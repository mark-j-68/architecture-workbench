package com.architectureworkbench.discovery;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DiscoveryInput(
        Path rootDirectory,
        List<DiscoveryOutput> priorOutputs,
        Map<String, String> parameters
) {
    public DiscoveryInput {
        rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        priorOutputs = List.copyOf(priorOutputs == null ? List.of() : priorOutputs);
        parameters = Map.copyOf(parameters == null ? Map.of() : parameters);
    }

    public static DiscoveryInput root(Path rootDirectory) {
        return new DiscoveryInput(rootDirectory, List.of(), Map.of());
    }

    public DiscoveryInput withPriorOutputs(List<DiscoveryOutput> outputs) {
        return new DiscoveryInput(rootDirectory, outputs, parameters);
    }
}
