package com.architectureworkbench.knowledgegraph;

import java.util.Map;
import java.util.Objects;

public record ProposedRelationshipAddition(
        ElementId sourceId,
        ElementId targetId,
        RelationshipType relationshipType,
        String label,
        Map<String, String> attributes
) implements ProposedGraphMutation {
    public ProposedRelationshipAddition {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        targetId = Objects.requireNonNull(targetId, "targetId");
        relationshipType = Objects.requireNonNull(relationshipType, "relationshipType");
        label = Objects.requireNonNullElse(label, relationshipType.name());
        attributes = Map.copyOf(Objects.requireNonNullElseGet(attributes, Map::of));
    }

    @Override
    public ProposedChangeType changeType() {
        return ProposedChangeType.RELATIONSHIP_ADDITION;
    }
}
