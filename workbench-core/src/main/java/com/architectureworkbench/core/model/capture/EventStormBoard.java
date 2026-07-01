package com.architectureworkbench.core.model.capture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A native, structured Event Storming board captured in the workbench. */
public class EventStormBoard {
    private String id;
    private String name;
    private String boundedContextHint;
    private String source = "tldraw";
    private Instant capturedAt = Instant.now();
    private List<EventStormSticky> stickies = new ArrayList<>();
    private List<EventStormLink> links = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBoundedContextHint() { return boundedContextHint; }
    public void setBoundedContextHint(String boundedContextHint) { this.boundedContextHint = boundedContextHint; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = Objects.requireNonNullElseGet(capturedAt, Instant::now); }
    public List<EventStormSticky> getStickies() { return stickies; }
    public void setStickies(List<EventStormSticky> stickies) { this.stickies = Objects.requireNonNullElseGet(stickies, ArrayList::new); }
    public List<EventStormLink> getLinks() { return links; }
    public void setLinks(List<EventStormLink> links) { this.links = Objects.requireNonNullElseGet(links, ArrayList::new); }
}
