package com.architectureworkbench.core.model.deployment;

import java.util.ArrayList;
import java.util.List;

public class DeploymentSection {
    private String target = "localstack";
    private String region = "eu-west-1";
    private List<DeploymentResource> resources = new ArrayList<>();

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public List<DeploymentResource> getResources() { return resources; }
    public void setResources(List<DeploymentResource> resources) { this.resources = resources == null ? new ArrayList<>() : resources; }
}
