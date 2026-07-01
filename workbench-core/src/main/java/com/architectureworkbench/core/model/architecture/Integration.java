package com.architectureworkbench.core.model.architecture;

public class Integration {
    private String source;
    private String target;
    private String style; // rest, event, file, batch
    private String description;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
