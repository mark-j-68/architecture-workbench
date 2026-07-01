package com.architectureworkbench.core.model.architecture;

import java.util.ArrayList;
import java.util.List;

public class ArchitectureSection {
    private List<Service> services = new ArrayList<>();
    private List<ExternalSystem> externalSystems = new ArrayList<>();
    private List<Integration> integrations = new ArrayList<>();

    public List<Service> getServices() { return services; }
    public void setServices(List<Service> services) { this.services = services == null ? new ArrayList<>() : services; }
    public List<ExternalSystem> getExternalSystems() { return externalSystems; }
    public void setExternalSystems(List<ExternalSystem> externalSystems) { this.externalSystems = externalSystems == null ? new ArrayList<>() : externalSystems; }
    public List<Integration> getIntegrations() { return integrations; }
    public void setIntegrations(List<Integration> integrations) { this.integrations = integrations == null ? new ArrayList<>() : integrations; }
}
