package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Evidence extends ArchitectureElement {
    public Evidence(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.EVIDENCE, name, description, attributes);
    }

    public String sourceUri() {
        return attributes().getOrDefault("sourceUri", "");
    }
}
