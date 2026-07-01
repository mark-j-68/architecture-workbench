package com.architectureworkbench.core.mcp;

import java.util.Map;

public record McpToolCall(String name, Map<String, Object> arguments) {}
