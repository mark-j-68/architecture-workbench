package com.architectureworkbench.core.model.validation;

public class ValidationFinding {
    private String ruleId;
    private Severity severity;
    private String message;
    private String location;

    public ValidationFinding() {}
    public ValidationFinding(String ruleId, Severity severity, String message, String location) {
        this.ruleId = ruleId; this.severity = severity; this.message = message; this.location = location;
    }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
