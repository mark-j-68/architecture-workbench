package com.architectureworkbench.workspace;

import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ElementId;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import com.architectureworkbench.knowledgegraph.ProposedChangeId;
import com.architectureworkbench.knowledgegraph.ProposedChangeStatus;
import com.architectureworkbench.knowledgegraph.ProposedChangeType;
import com.architectureworkbench.knowledgegraph.ProposedElementAddition;
import com.architectureworkbench.knowledgegraph.ProposedGraphMutation;
import com.architectureworkbench.knowledgegraph.ProposedRelationshipAddition;
import com.architectureworkbench.knowledgegraph.RelationshipType;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FileProposedChangeRepository implements ProposedChangeRepository {
    private static final TypeReference<List<ProposedChangeJson>> CHANGE_LIST = new TypeReference<>() {};
    private final Path root;

    public FileProposedChangeRepository() {
        this(FileWorkspaceStorage.defaultRoot());
    }

    public FileProposedChangeRepository(Path root) {
        this.root = root;
    }

    @Override
    public synchronized ProposedArchitectureChange save(ProposedArchitectureChange change) {
        List<ProposedArchitectureChange> existing = findByWorkspaceId(change.workspaceId()).stream()
                .filter(candidate -> !candidate.id().equals(change.id()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        existing.add(change);
        WorkspaceJson.write(proposedChangesFile(change.workspaceId()), existing.stream().map(ProposedChangeJson::from).toList());
        return change;
    }

    @Override
    public synchronized Optional<ProposedArchitectureChange> findById(String proposedChangeId) {
        return allChanges().stream()
                .filter(change -> change.id().value().equals(proposedChangeId))
                .findFirst();
    }

    @Override
    public synchronized List<ProposedArchitectureChange> findByWorkspaceId(String workspaceId) {
        Path path = proposedChangesFile(workspaceId);
        if (!Files.exists(path)) {
            return List.of();
        }
        return WorkspaceJson.read(path, CHANGE_LIST).stream().map(ProposedChangeJson::toDomain).toList();
    }

    private List<ProposedArchitectureChange> allChanges() {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (java.util.stream.Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .flatMap(path -> findByWorkspaceId(path.getFileName().toString()).stream())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list proposed-change storage directory: " + root, e);
        }
    }

    private Path proposedChangesFile(String workspaceId) {
        return root.resolve(workspaceId).resolve("proposed-changes.json");
    }

    private record ProposedChangeJson(
            String id,
            ProposedChangeType type,
            ProposedChangeStatus status,
            String workspaceId,
            String correlationId,
            String recommendationId,
            List<String> findingIds,
            List<String> evidenceIds,
            Instant createdAt,
            Instant decidedAt,
            String decisionRationale,
            ArchitectureElementType elementType,
            String elementName,
            String elementDescription,
            Map<String, String> elementAttributes,
            String sourceId,
            String targetId,
            RelationshipType relationshipType,
            String relationshipLabel,
            Map<String, String> relationshipAttributes
    ) {
        static ProposedChangeJson from(ProposedArchitectureChange change) {
            if (change.mutation() instanceof ProposedElementAddition elementAddition) {
                return new ProposedChangeJson(
                        change.id().value(),
                        change.type(),
                        change.status(),
                        change.workspaceId(),
                        change.correlationId().value(),
                        change.recommendationId(),
                        change.findingIds(),
                        change.evidenceIds(),
                        change.createdAt(),
                        change.decidedAt(),
                        change.decisionRationale(),
                        elementAddition.elementType(),
                        elementAddition.name(),
                        elementAddition.description(),
                        elementAddition.attributes(),
                        null,
                        null,
                        null,
                        null,
                        Map.of()
                );
            }
            ProposedRelationshipAddition relationshipAddition = (ProposedRelationshipAddition) change.mutation();
            return new ProposedChangeJson(
                    change.id().value(),
                    change.type(),
                    change.status(),
                    change.workspaceId(),
                    change.correlationId().value(),
                    change.recommendationId(),
                    change.findingIds(),
                    change.evidenceIds(),
                    change.createdAt(),
                    change.decidedAt(),
                    change.decisionRationale(),
                    null,
                    null,
                    null,
                    Map.of(),
                    relationshipAddition.sourceId().value(),
                    relationshipAddition.targetId().value(),
                    relationshipAddition.relationshipType(),
                    relationshipAddition.label(),
                    relationshipAddition.attributes()
            );
        }

        ProposedArchitectureChange toDomain() {
            ProposedGraphMutation mutation = switch (type) {
                case ELEMENT_ADDITION -> new ProposedElementAddition(elementType, elementName, elementDescription, elementAttributes);
                case RELATIONSHIP_ADDITION -> new ProposedRelationshipAddition(
                        ElementId.of(sourceId),
                        ElementId.of(targetId),
                        relationshipType,
                        relationshipLabel,
                        relationshipAttributes
                );
            };
            return new ProposedArchitectureChange(
                    new ProposedChangeId(id),
                    type,
                    status,
                    mutation,
                    workspaceId,
                    new CorrelationId(correlationId),
                    recommendationId,
                    findingIds,
                    evidenceIds,
                    createdAt,
                    decidedAt,
                    decisionRationale
            );
        }
    }
}
