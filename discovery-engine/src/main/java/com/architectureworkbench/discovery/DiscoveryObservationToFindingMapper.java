package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.ConfidenceCalculator;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Severity;
import java.util.List;

public class DiscoveryObservationToFindingMapper {
    private final ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator();

    public Finding mapArtifactObservation(DiscoveredArtifact artifact, Observation observation) {
        return new Finding(
                "finding-" + artifact.artifactId(),
                Severity.INFO,
                "DISCOVERY_ARTIFACT",
                "Discovery recorded %s '%s'.".formatted(artifact.type().name(), artifact.name()),
                List.of(observation),
                confidenceCalculator.calculateObservationConfidence(List.of(observation))
        );
    }

    public Finding mapHealthcheckFinding(DiscoveryFinding finding, List<Observation> observations) {
        return new Finding(
                "finding-" + finding.findingId(),
                severity(finding.severity()),
                "DISCOVERY_HEALTHCHECK:" + finding.ruleId(),
                finding.message(),
                observations,
                confidenceCalculator.calculateObservationConfidence(observations)
        );
    }

    private static Severity severity(DiscoveryFindingSeverity severity) {
        return switch (severity) {
            case INFO -> Severity.INFO;
            case WARNING -> Severity.WARNING;
            case ERROR -> Severity.ERROR;
        };
    }
}
