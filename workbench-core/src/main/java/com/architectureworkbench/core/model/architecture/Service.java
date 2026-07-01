package com.architectureworkbench.core.model.architecture;

import java.util.ArrayList;
import java.util.List;

public class Service {
    private String name;
    private String type = "application-service";
    private List<String> ownsAggregates = new ArrayList<>();
    private List<String> exposesApis = new ArrayList<>();
    private List<String> publishesEvents = new ArrayList<>();
    private List<String> subscribesToEvents = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<String> getOwnsAggregates() { return ownsAggregates; }
    public void setOwnsAggregates(List<String> ownsAggregates) { this.ownsAggregates = ownsAggregates == null ? new ArrayList<>() : ownsAggregates; }
    public List<String> getExposesApis() { return exposesApis; }
    public void setExposesApis(List<String> exposesApis) { this.exposesApis = exposesApis == null ? new ArrayList<>() : exposesApis; }
    public List<String> getPublishesEvents() { return publishesEvents; }
    public void setPublishesEvents(List<String> publishesEvents) { this.publishesEvents = publishesEvents == null ? new ArrayList<>() : publishesEvents; }
    public List<String> getSubscribesToEvents() { return subscribesToEvents; }
    public void setSubscribesToEvents(List<String> subscribesToEvents) { this.subscribesToEvents = subscribesToEvents == null ? new ArrayList<>() : subscribesToEvents; }
}
