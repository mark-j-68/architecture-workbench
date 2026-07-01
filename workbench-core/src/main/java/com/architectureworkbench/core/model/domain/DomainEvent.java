package com.architectureworkbench.core.model.domain;

import java.util.ArrayList;
import java.util.List;

public class DomainEvent {
    private String name;
    private String emittedByAggregate;
    private List<String> payload = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmittedByAggregate() { return emittedByAggregate; }
    public void setEmittedByAggregate(String emittedByAggregate) { this.emittedByAggregate = emittedByAggregate; }
    public List<String> getPayload() { return payload; }
    public void setPayload(List<String> payload) { this.payload = payload == null ? new ArrayList<>() : payload; }
}
