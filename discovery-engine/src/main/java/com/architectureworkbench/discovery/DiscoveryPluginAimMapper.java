package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Observation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscoveryPluginAimMapper {
    public Evidence mapEvidence(DiscoveryEvidence evidence) {
        return new Evidence(
                "evidence-" + evidence.evidenceId(),
                "discovery-plugin:" + evidence.evidenceType(),
                "%s via %s".formatted(evidence.provenance(), evidence.source()),
                evidence.confidence().value(),
                evidence.discoveredAt(),
                evidence.references(),
                List.of(evidence.evidenceId())
        );
    }

    public Observation mapObservation(DiscoveryObservation observation, List<Evidence> mappedEvidence) {
        Map<String, Evidence> evidenceBySourceId = mappedEvidence.stream()
                .filter(evidence -> !evidence.supportingArtifacts().isEmpty())
                .collect(Collectors.toMap(evidence -> evidence.supportingArtifacts().get(0), evidence -> evidence, (left, right) -> left));
        List<Evidence> relatedEvidence = observation.relatedEvidenceIds().stream()
                .map(evidenceBySourceId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new Observation(
                "observation-" + observation.observationId(),
                "discovery-plugin:" + observation.observationType(),
                observation.description(),
                relatedEvidence,
                List.of()
        );
    }
}
