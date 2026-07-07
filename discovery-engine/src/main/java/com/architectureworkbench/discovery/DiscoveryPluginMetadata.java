package com.architectureworkbench.discovery;

import java.util.List;
import java.util.Objects;

public record DiscoveryPluginMetadata(
        DiscoveryPluginId id,
        String name,
        String version,
        String category,
        List<String> supportedTechnologies,
        List<DiscoveryPluginCapability> capabilities,
        List<DiscoveryPluginDependency> dependencies,
        boolean deterministic
) {
    public DiscoveryPluginMetadata {
        id = Objects.requireNonNull(id, "id");
        name = required(name, "name");
        version = required(version, "version");
        category = required(category, "category");
        supportedTechnologies = List.copyOf(supportedTechnologies == null ? List.of() : supportedTechnologies);
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
