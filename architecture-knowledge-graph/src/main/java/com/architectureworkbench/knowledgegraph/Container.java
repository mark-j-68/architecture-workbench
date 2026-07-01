package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Container extends ArchitectureElement {
    public Container(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.CONTAINER, name, description, attributes);
    }
}
