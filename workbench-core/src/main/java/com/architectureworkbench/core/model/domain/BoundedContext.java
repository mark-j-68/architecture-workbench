package com.architectureworkbench.core.model.domain;

import java.util.ArrayList;
import java.util.List;

public class BoundedContext {
    private String name;
    private String description;
    private List<Aggregate> aggregates = new ArrayList<>();
    private List<Command> commands = new ArrayList<>();
    private List<DomainEvent> events = new ArrayList<>();
    private List<Policy> policies = new ArrayList<>();
    private List<ReadModel> readModels = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Aggregate> getAggregates() { return aggregates; }
    public void setAggregates(List<Aggregate> aggregates) { this.aggregates = aggregates == null ? new ArrayList<>() : aggregates; }
    public List<Command> getCommands() { return commands; }
    public void setCommands(List<Command> commands) { this.commands = commands == null ? new ArrayList<>() : commands; }
    public List<DomainEvent> getEvents() { return events; }
    public void setEvents(List<DomainEvent> events) { this.events = events == null ? new ArrayList<>() : events; }
    public List<Policy> getPolicies() { return policies; }
    public void setPolicies(List<Policy> policies) { this.policies = policies == null ? new ArrayList<>() : policies; }
    public List<ReadModel> getReadModels() { return readModels; }
    public void setReadModels(List<ReadModel> readModels) { this.readModels = readModels == null ? new ArrayList<>() : readModels; }
}
