package com.architectureworkbench.core.model.domain;

import java.util.ArrayList;
import java.util.List;

public class ReadModel {
    private String name;
    private String description;
    private List<String> populatedByEvents = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getPopulatedByEvents() { return populatedByEvents; }
    public void setPopulatedByEvents(List<String> populatedByEvents) { this.populatedByEvents = populatedByEvents == null ? new ArrayList<>() : populatedByEvents; }
}
