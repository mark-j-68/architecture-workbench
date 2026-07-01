package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Capability extends ArchitectureElement {
    public Capability(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.CAPABILITY, name, description, attributes);
    }
}
