package com.architectureworkbench.intelligence;

public record Concern(
        String id,
        String name,
        String description,
        String category
) {
    public Concern {
        id = AimIds.id("concern", id);
        name = AimIds.required(name, "name");
        description = AimIds.required(description, "description");
        category = AimIds.required(category, "category");
    }
}
