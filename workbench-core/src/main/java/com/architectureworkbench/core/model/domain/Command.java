package com.architectureworkbench.core.model.domain;

public class Command {
    private String name;
    private String handledByAggregate;
    private String intent;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHandledByAggregate() { return handledByAggregate; }
    public void setHandledByAggregate(String handledByAggregate) { this.handledByAggregate = handledByAggregate; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
}
