package com.architectureworkbench.discovery;

import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.LifecycleStatus;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.ProposedElementAddition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscoveryProposedChangeMapper {
    private final ProposedChangeService proposedChangeService;

    public DiscoveryProposedChangeMapper(ProposedChangeService proposedChangeService) {
        this.proposedChangeService = proposedChangeService;
    }

    public DiscoveryProposalResult proposeChanges(
            DiscoveryContext context,
            List<DiscoveredArtifact> artifacts,
            List<Finding> findings,
            CorrelationId correlationId
    ) {
        List<Recommendation> recommendations = new ArrayList<>();
        List<ProposedArchitectureChange> changes = new ArrayList<>();
        for (DiscoveredArtifact artifact : artifacts) {
            findings.stream()
                    .filter(finding -> finding.supportingObservations().stream()
                            .flatMap(observation -> observation.relatedEvidence().stream())
                            .flatMap(evidence -> evidence.supportingArtifacts().stream())
                            .anyMatch(ref -> ref.equals(artifact.artifactId())))
                    .findFirst()
                    .ifPresent(finding -> {
                        Recommendation recommendation = recommendationFor(artifact, finding);
                        recommendations.add(recommendation);
                        changes.add(proposedChangeService.proposeElementAddition(
                                context.graph().graphId(),
                                correlationId,
                                new ProposedElementAddition(
                                        elementTypeFor(artifact.type()),
                                        artifact.name(),
                                        "Proposed from discovery evidence at " + artifact.path(),
                                        Map.of(
                                                "source", "discovery",
                                                "artifactId", artifact.artifactId(),
                                                "artifactType", artifact.type().name(),
                                                "path", artifact.path()
                                        )
                                ),
                                recommendation.id(),
                                List.of(finding.id()),
                                evidenceIds(finding)
                        ));
                    });
        }
        return new DiscoveryProposalResult(recommendations, changes);
    }

    private static Recommendation recommendationFor(DiscoveredArtifact artifact, Finding finding) {
        return new Recommendation(
                "recommendation-discovery-" + artifact.artifactId(),
                "Consider adding discovered %s '%s' to the architecture graph.".formatted(artifact.type().name(), artifact.name()),
                "Discovery recorded evidence for this artifact; graph mutation requires explicit acceptance.",
                List.of(),
                List.of(finding),
                "Improves discovered-system architecture visibility.",
                "Low; validate naming and ownership before acceptance.",
                finding.confidence(),
                LifecycleStatus.PROPOSED
        );
    }

    private static List<String> evidenceIds(Finding finding) {
        return finding.supportingObservations().stream()
                .flatMap(observation -> observation.relatedEvidence().stream())
                .map(com.architectureworkbench.intelligence.Evidence::id)
                .distinct()
                .toList();
    }

    private static ArchitectureElementType elementTypeFor(DiscoveredArtifactType type) {
        return switch (type) {
            case MAVEN_MODULE, POM_FILE, DOCKER_FILE -> ArchitectureElementType.CONTAINER;
            case SPRING_CONTROLLER, SPRING_SERVICE, REPOSITORY_CLASS, CONFIGURATION_FILE -> ArchitectureElementType.COMPONENT;
            case JAVA_PACKAGE -> ArchitectureElementType.COMPONENT;
            case README_DOC, DOC_FILE, ADR_DIRECTORY, TEST_DIRECTORY -> ArchitectureElementType.EVIDENCE;
        };
    }
}
