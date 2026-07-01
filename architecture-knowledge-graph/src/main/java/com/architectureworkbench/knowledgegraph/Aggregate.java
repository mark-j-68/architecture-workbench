package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Aggregate extends ArchitectureElement {
    public Aggregate(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.AGGREGATE, name, description, attributes);
    }

    public String rootEntity() {
        return attributes().getOrDefault("rootEntity", "");
    }
}
