package com.architectureworkbench.core.model.capture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A typed sticky note from an Event Storming session. */
public class EventStormSticky {
    private String id;
    private EventStormStickyType type = EventStormStickyType.UNKNOWN;
    private String text;
    private CanvasPosition position = new CanvasPosition();
    private double width = 220;
    private double height = 120;
    private String colour;
    private String provenance = "human-authored";
    private double confidence = 1.0;
    private Map<String, String> metadata = new LinkedHashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public EventStormStickyType getType() { return type; }
    public void setType(EventStormStickyType type) { this.type = Objects.requireNonNullElse(type, EventStormStickyType.UNKNOWN); }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public CanvasPosition getPosition() { return position; }
    public void setPosition(CanvasPosition position) { this.position = Objects.requireNonNullElseGet(position, CanvasPosition::new); }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = Objects.requireNonNullElseGet(metadata, LinkedHashMap::new); }
}
