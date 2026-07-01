package com.architectureworkbench.knowledgegraph;

import java.util.Map;
import java.util.Objects;

public record ProposedElementAddition(
        ArchitectureElementType elementType,
        String name,
        String description,
        Map<String, String> attributes
) implements ProposedGraphMutation {
    public ProposedElementAddition {
        elementType = Objects.requireNonNull(elementType, "elementType");
        name = ArchitectureElement.requireText(name, "name");
        description = Objects.requireNonNullElse(description, "");
        attributes = Map.copyOf(Objects.requireNonNullElseGet(attributes, Map::of));
    }

    @Override
    public ProposedChangeType changeType() {
        return ProposedChangeType.ELEMENT_ADDITION;
    }
}
