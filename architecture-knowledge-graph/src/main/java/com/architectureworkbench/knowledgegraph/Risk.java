package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Risk extends ArchitectureElement {
    public Risk(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.RISK, name, description, attributes);
    }
}
