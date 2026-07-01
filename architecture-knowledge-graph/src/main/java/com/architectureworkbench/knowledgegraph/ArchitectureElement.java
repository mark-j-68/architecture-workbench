package com.architectureworkbench.knowledgegraph;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class ArchitectureElement {
    private final ElementId id;
    private final ArchitectureElementType type;
    private final String name;
    private final String description;
    private final Map<String, String> attributes;
    private final Instant createdAt;

    protected ArchitectureElement(ElementId id, ArchitectureElementType type, String name, String description, Map<String, String> attributes) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNullElseGet(attributes, Map::of)));
        this.createdAt = Instant.now();
    }

    public ElementId id() { return id; }
    public ArchitectureElementType type() { return type; }
    public String name() { return name; }
    public String description() { return description; }
    public Map<String, String> attributes() { return attributes; }
    public Instant createdAt() { return createdAt; }

    protected static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
