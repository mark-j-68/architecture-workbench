package com.architectureworkbench.knowledgegraphmcp;

import java.util.Map;

public record KnowledgeGraphMcpToolCall(String name, Map<String, Object> arguments) {
    public KnowledgeGraphMcpToolCall {
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
    }
}
