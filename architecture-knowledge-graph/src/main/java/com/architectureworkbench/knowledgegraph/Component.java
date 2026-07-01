package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Component extends ArchitectureElement {
    public Component(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.COMPONENT, name, description, attributes);
    }
}
