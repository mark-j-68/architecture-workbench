package com.architectureworkbench.knowledgegraph;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Projection(
        ProjectionType type,
        Instant generatedAt,
        List<String> sourceElementRefs,
        List<String> sourceRelationshipRefs,
        ProjectionPayload payload
) {}
