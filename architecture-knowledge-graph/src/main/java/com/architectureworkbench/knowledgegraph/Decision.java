package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Decision extends ArchitectureElement {
    public Decision(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.DECISION, name, description, attributes);
    }

    public String outcome() {
        return attributes().getOrDefault("outcome", "");
    }
}
