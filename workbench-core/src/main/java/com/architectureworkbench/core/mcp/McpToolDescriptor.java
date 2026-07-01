package com.architectureworkbench.core.mcp;

import java.util.Map;

public record McpToolDescriptor(
        String name,
        String description,
        Map<String, String> inputSchema,
        boolean mutatesArchitectureModel
) {}
