package com.architectureworkbench.knowledgegraph;

public record ValidationFinding(
        String ruleId,
        ValidationSeverity severity,
        String message,
        String elementRef
) {}
