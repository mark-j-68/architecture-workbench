package com.architectureworkbench.knowledgegraph;

import java.util.ArrayList;
import java.util.List;

public class ValidationReport {
    private final List<ValidationFinding> findings = new ArrayList<>();

    public List<ValidationFinding> findings() {
        return List.copyOf(findings);
    }

    public boolean hasErrors() {
        return findings.stream().anyMatch(finding -> finding.severity() == ValidationSeverity.ERROR);
    }

    void add(ValidationFinding finding) {
        findings.add(finding);
    }
}
