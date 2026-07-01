package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class ArchitectureElementFactory {
    public ArchitectureElement create(ArchitectureElementType type, String name, String description, Map<String, String> attributes) {
        ElementId id = ElementId.newId(type);
        return switch (type) {
            case DOMAIN_EVENT -> new DomainEvent(id, name, description, attributes);
            case COMMAND -> new Command(id, name, description, attributes);
            case AGGREGATE -> new Aggregate(id, name, description, attributes);
            case BOUNDED_CONTEXT -> new BoundedContext(id, name, description, attributes);
            case CAPABILITY -> new Capability(id, name, description, attributes);
            case POLICY -> new Policy(id, name, description, attributes);
            case DECISION -> new Decision(id, name, description, attributes);
            case RISK -> new Risk(id, name, description, attributes);
            case SYSTEM -> new System(id, name, description, attributes);
            case CONTAINER -> new Container(id, name, description, attributes);
            case COMPONENT -> new Component(id, name, description, attributes);
            case ADR -> new ADR(id, name, description, attributes);
            case ARCHITECTURE_REVIEW -> new ArchitectureReview(id, name, description, attributes);
            case EVIDENCE -> new Evidence(id, name, description, attributes);
        };
    }
}
