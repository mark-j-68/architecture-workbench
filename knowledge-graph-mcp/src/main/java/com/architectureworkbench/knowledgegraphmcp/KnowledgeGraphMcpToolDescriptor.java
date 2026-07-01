package com.architectureworkbench.knowledgegraphmcp;

import java.util.Map;

public record KnowledgeGraphMcpToolDescriptor(
        String name,
        String description,
        Map<String, String> inputSchema,
        boolean mutatesGraph
) {}
