package com.architectureworkbench.discovery;

import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.CreateArchitectureElementCommand;
import java.util.Map;

public class DiscoveryGraphMapper {
    private final ArchitectureElementService elementService;

    public DiscoveryGraphMapper(ArchitectureElementService elementService) {
        this.elementService = elementService;
    }

    public void mapArtifact(ArchitectureKnowledgeGraph graph, DiscoveredArtifact artifact, String actorRef) {
        ArchitectureElementType type = switch (artifact.type()) {
            case MAVEN_MODULE, SPRING_CONTROLLER, SPRING_SERVICE, REPOSITORY_CLASS, JAVA_PACKAGE -> ArchitectureElementType.COMPONENT;
            case DOCKER_FILE -> ArchitectureElementType.CONTAINER;
            case POM_FILE, CONFIGURATION_FILE, README_DOC, DOC_FILE, ADR_DIRECTORY, TEST_DIRECTORY -> ArchitectureElementType.EVIDENCE;
        };
        elementService.createElement(graph, new CreateArchitectureElementCommand(
                type,
                artifact.name(),
                "Discovered %s at %s".formatted(artifact.type(), artifact.path()),
                Map.of(
                        "discoveryArtifactId", artifact.artifactId(),
                        "discoveryArtifactType", artifact.type().name(),
                        "path", artifact.path()
                ),
                actorRef
        ));
    }
}
