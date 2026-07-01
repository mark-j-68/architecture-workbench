package com.architectureworkbench.knowledgegraph;

import java.util.List;

public record EventStormingProjection(
        List<String> events,
        List<String> commands,
        List<String> aggregates,
        List<String> policies
) implements ProjectionPayload {
    public EventStormingProjection {
        events = List.copyOf(events == null ? List.of() : events);
        commands = List.copyOf(commands == null ? List.of() : commands);
        aggregates = List.copyOf(aggregates == null ? List.of() : aggregates);
        policies = List.copyOf(policies == null ? List.of() : policies);
    }
}
