package com.architectureworkbench.discovery;

import java.util.List;

public record DiscoveryOutput(
        List<DiscoveryEvidence> evidence,
        List<DiscoveryObservation> observations,
        List<String> diagnostics
) {
    public DiscoveryOutput {
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        observations = List.copyOf(observations == null ? List.of() : observations);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public static DiscoveryOutput empty() {
        return new DiscoveryOutput(List.of(), List.of(), List.of());
    }
}
