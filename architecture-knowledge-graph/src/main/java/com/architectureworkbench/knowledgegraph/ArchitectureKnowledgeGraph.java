package com.architectureworkbench.knowledgegraph;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ArchitectureKnowledgeGraph {
    private final String graphId;
    private final Map<ElementId, ArchitectureElement> elements = new LinkedHashMap<>();
    private final Map<String, Relationship> relationships = new LinkedHashMap<>();

    public ArchitectureKnowledgeGraph(String graphId) {
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException("Graph id is required.");
        }
        this.graphId = graphId;
    }

    public String graphId() { return graphId; }

    public Collection<ArchitectureElement> elements() {
        return List.copyOf(elements.values());
    }

    public Collection<Relationship> relationships() {
        return List.copyOf(relationships.values());
    }

    public Optional<ArchitectureElement> element(ElementId id) {
        return Optional.ofNullable(elements.get(id));
    }

    public List<ArchitectureElement> elementsOfType(ArchitectureElementType type) {
        return elements.values().stream().filter(element -> element.type() == type).toList();
    }

    public List<Relationship> outgoing(ElementId sourceId, RelationshipType type) {
        return relationships.values().stream()
                .filter(relationship -> relationship.sourceId().equals(sourceId))
                .filter(relationship -> type == null || relationship.type() == type)
                .toList();
    }

    public List<Relationship> incoming(ElementId targetId, RelationshipType type) {
        return relationships.values().stream()
                .filter(relationship -> relationship.targetId().equals(targetId))
                .filter(relationship -> type == null || relationship.type() == type)
                .toList();
    }

    boolean contains(ElementId id) {
        return elements.containsKey(id);
    }

    void addElement(ArchitectureElement element) {
        Objects.requireNonNull(element, "element");
        if (elements.containsKey(element.id())) {
            throw new IllegalArgumentException("Element already exists: " + element.id().value());
        }
        elements.put(element.id(), element);
    }

    void addRelationship(Relationship relationship) {
        Objects.requireNonNull(relationship, "relationship");
        if (!elements.containsKey(relationship.sourceId()) || !elements.containsKey(relationship.targetId())) {
            throw new IllegalArgumentException("Both relationship endpoints must exist in the graph.");
        }
        relationships.put(relationship.id(), relationship);
    }
}
