package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class Command extends ArchitectureElement {
    public Command(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.COMMAND, name, description, attributes);
    }
}
