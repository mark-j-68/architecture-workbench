package com.architectureworkbench.core.model.domain;

import java.util.ArrayList;
import java.util.List;

public class Aggregate {
    private String name;
    private String rootEntity;
    private String description;
    private List<String> invariants = new ArrayList<>();
    private List<String> valueObjects = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRootEntity() { return rootEntity; }
    public void setRootEntity(String rootEntity) { this.rootEntity = rootEntity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getInvariants() { return invariants; }
    public void setInvariants(List<String> invariants) { this.invariants = invariants == null ? new ArrayList<>() : invariants; }
    public List<String> getValueObjects() { return valueObjects; }
    public void setValueObjects(List<String> valueObjects) { this.valueObjects = valueObjects == null ? new ArrayList<>() : valueObjects; }
}
