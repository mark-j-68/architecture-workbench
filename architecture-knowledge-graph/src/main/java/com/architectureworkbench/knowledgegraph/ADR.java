package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class ADR extends ArchitectureElement {
    public ADR(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.ADR, name, description, attributes);
    }

    public String status() {
        return attributes().getOrDefault("status", "Proposed");
    }
}
