package com.architectureworkbench.core.model.capture;

/** Directed or semantic link between two Event Storm stickies. */
public class EventStormLink {
    private String id;
    private String fromStickyId;
    private String toStickyId;
    private String relationship;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromStickyId() { return fromStickyId; }
    public void setFromStickyId(String fromStickyId) { this.fromStickyId = fromStickyId; }
    public String getToStickyId() { return toStickyId; }
    public void setToStickyId(String toStickyId) { this.toStickyId = toStickyId; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
}
