package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class BoundedContext extends ArchitectureElement {
    public BoundedContext(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.BOUNDED_CONTEXT, name, description, attributes);
    }
}
