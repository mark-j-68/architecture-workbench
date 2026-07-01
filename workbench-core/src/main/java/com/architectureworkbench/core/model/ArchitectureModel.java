package com.architectureworkbench.core.model;

import com.architectureworkbench.core.model.architecture.ArchitectureSection;
import com.architectureworkbench.core.model.capture.CaptureSection;
import com.architectureworkbench.core.model.deployment.DeploymentSection;
import com.architectureworkbench.core.model.domain.DomainSection;
import com.architectureworkbench.core.model.governance.GovernanceSection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Legacy M2 sectioned representation of an architecture workspace.
 *
 * <p>This type is no longer the canonical runtime boundary. M3.1 establishes
 * {@code ArchitectureKnowledgeGraph} as the platform core. Keep this model for
 * compatibility and migration only; new API, UI, MCP, discovery, healthcheck,
 * review, and provider integrations must use the graph boundary.
 */
@Deprecated
public class ArchitectureModel {
    private ModelMetadata metadata = new ModelMetadata();
    private CaptureSection capture = new CaptureSection();
    private DomainSection domain = new DomainSection();
    private ArchitectureSection architecture = new ArchitectureSection();
    private DeploymentSection deployment = new DeploymentSection();
    private GovernanceSection governance = new GovernanceSection();
    private List<String> tags = new ArrayList<>();

    public ModelMetadata getMetadata() { return metadata; }
    public void setMetadata(ModelMetadata metadata) { this.metadata = Objects.requireNonNullElseGet(metadata, ModelMetadata::new); }
    public CaptureSection getCapture() { return capture; }
    public void setCapture(CaptureSection capture) { this.capture = Objects.requireNonNullElseGet(capture, CaptureSection::new); }
    public DomainSection getDomain() { return domain; }
    public void setDomain(DomainSection domain) { this.domain = Objects.requireNonNullElseGet(domain, DomainSection::new); }
    public ArchitectureSection getArchitecture() { return architecture; }
    public void setArchitecture(ArchitectureSection architecture) { this.architecture = Objects.requireNonNullElseGet(architecture, ArchitectureSection::new); }
    public DeploymentSection getDeployment() { return deployment; }
    public void setDeployment(DeploymentSection deployment) { this.deployment = Objects.requireNonNullElseGet(deployment, DeploymentSection::new); }
    public GovernanceSection getGovernance() { return governance; }
    public void setGovernance(GovernanceSection governance) { this.governance = Objects.requireNonNullElseGet(governance, GovernanceSection::new); }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = Objects.requireNonNullElseGet(tags, ArrayList::new); }
}
