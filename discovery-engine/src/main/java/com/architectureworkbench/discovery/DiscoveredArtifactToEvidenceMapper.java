package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import java.time.Instant;
import java.util.List;

public class DiscoveredArtifactToEvidenceMapper {
    public Evidence map(DiscoveryContext context, DiscoveredArtifact artifact) {
        return new Evidence(
                "evidence-" + artifact.artifactId(),
                "discovery:" + artifact.type().name(),
                "Discovered from %s at %s".formatted(context.source().type(), context.source().uri()),
                confidenceFor(artifact.type()),
                Instant.now(),
                List.of(artifact.path()),
                List.of(artifact.artifactId())
        );
    }

    public Evidence sourceEvidence(DiscoveryContext context) {
        return new Evidence(
                "evidence-" + context.runId().value(),
                "discovery:" + context.source().type().name(),
                "Discovery source %s".formatted(context.source().uri()),
                0.6,
                Instant.now(),
                List.of(context.source().uri()),
                List.of(context.source().sourceId())
        );
    }

    private static double confidenceFor(DiscoveredArtifactType type) {
        return switch (type) {
            case POM_FILE, MAVEN_MODULE, SPRING_CONTROLLER, SPRING_SERVICE, REPOSITORY_CLASS, DOCKER_FILE -> 0.9;
            case README_DOC, DOC_FILE, ADR_DIRECTORY, TEST_DIRECTORY -> 0.85;
            case JAVA_PACKAGE, CONFIGURATION_FILE -> 0.8;
        };
    }
}
