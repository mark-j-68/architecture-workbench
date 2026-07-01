package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Observation;
import java.util.List;

public class DiscoveryEvidenceToObservationMapper {
    public Observation map(DiscoveredArtifact artifact, Evidence evidence) {
        return new Observation(
                "observation-" + artifact.artifactId(),
                "discovery",
                "Discovered %s '%s' at %s.".formatted(artifact.type().name(), artifact.name(), artifact.path()),
                List.of(evidence),
                List.of()
        );
    }
}
