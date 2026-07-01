package com.architectureworkbench.core.model.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Capture artefacts that precede the canonical domain model, such as Event Storm boards and imported images. */
public class CaptureSection {
    private List<EventStormBoard> eventStormBoards = new ArrayList<>();

    public List<EventStormBoard> getEventStormBoards() { return eventStormBoards; }
    public void setEventStormBoards(List<EventStormBoard> eventStormBoards) {
        this.eventStormBoards = Objects.requireNonNullElseGet(eventStormBoards, ArrayList::new);
    }
}
