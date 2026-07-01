package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record C4Projection(
        List<String> systems,
        List<String> containers,
        List<String> components,
        List<String> relationships
) implements ProjectionPayload {
    public C4Projection {
        systems = List.copyOf(systems == null ? List.of() : systems);
        containers = List.copyOf(containers == null ? List.of() : containers);
        components = List.copyOf(components == null ? List.of() : components);
        relationships = List.copyOf(relationships == null ? List.of() : relationships);
    }
}
