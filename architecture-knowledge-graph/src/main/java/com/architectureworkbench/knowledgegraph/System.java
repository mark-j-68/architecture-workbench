package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class System extends ArchitectureElement {
    public System(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.SYSTEM, name, description, attributes);
    }
}
