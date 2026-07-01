package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class DomainEvent extends ArchitectureElement {
    public DomainEvent(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.DOMAIN_EVENT, name, description, attributes);
    }
}
