package com.architectureworkbench.knowledgegraph;

public sealed interface ProjectionPayload permits
        ReactFlowProjection,
        C4Projection,
        EventStormingProjection,
        AdrProjection,
        OpenApiProjection,
        ReviewBoardProjection,
        BpmnProjection,
        DmnProjection {
}
