package com.architectureworkbench.core.model.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidationReport {
    private List<ValidationFinding> findings = new ArrayList<>();
    public List<ValidationFinding> getFindings() { return findings; }
    public void setFindings(List<ValidationFinding> findings) { this.findings = findings == null ? new ArrayList<>() : findings; }
    public boolean hasErrors() { return findings.stream().anyMatch(f -> f.getSeverity() == Severity.ERROR); }
    public void add(ValidationFinding finding) { findings.add(finding); }
}
