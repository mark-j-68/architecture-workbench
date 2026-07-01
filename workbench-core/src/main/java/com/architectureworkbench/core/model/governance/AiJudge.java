package com.architectureworkbench.core.model.governance;

public class AiJudge {
    private String id;
    private String provider;
    private String model;
    private String role;
    private boolean enabled = true;

    public AiJudge() {}
    public AiJudge(String id, String provider, String model, String role) {
        this.id = id; this.provider = provider; this.model = model; this.role = role;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
