package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class ArchitectureReview extends ArchitectureElement {
    public ArchitectureReview(ElementId id, String name, String description, Map<String, String> attributes) {
        super(id, ArchitectureElementType.ARCHITECTURE_REVIEW, name, description, attributes);
    }
}
