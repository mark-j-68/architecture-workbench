package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Policy extends ArchitectureElement {
    public Policy(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.POLICY, name, description, attributes);
    }
}
