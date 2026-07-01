package com.architectureworkbench.core.model.domain;

public class Policy {
    private String name;
    private String triggerEvent;
    private String condition;
    private String issuesCommand;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getIssuesCommand() { return issuesCommand; }
    public void setIssuesCommand(String issuesCommand) { this.issuesCommand = issuesCommand; }
}
