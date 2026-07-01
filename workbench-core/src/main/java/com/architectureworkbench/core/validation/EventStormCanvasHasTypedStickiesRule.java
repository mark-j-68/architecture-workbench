package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.capture.EventStormStickyType;
import com.architectureworkbench.core.model.validation.Severity;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.ArrayList;
import java.util.List;

/** Ensures native Event Storm boards have enough typed content to be useful for model extraction. */
public class EventStormCanvasHasTypedStickiesRule implements ValidationRule {
    @Override
    public String id() { return "CAPTURE-001"; }

    @Override
    public String description() { return "Event Storm boards with stickies should use recognised Event Storming sticky types."; }

    @Override
    public List<ValidationFinding> validate(ArchitectureModel model) {
        var findings = new ArrayList<ValidationFinding>();
        for (var board : model.getCapture().getEventStormBoards()) {
            long typed = board.getStickies().stream()
                    .filter(s -> s.getType() != EventStormStickyType.UNKNOWN && s.getType() != EventStormStickyType.COMMENT)
                    .count();
            if (!board.getStickies().isEmpty() && typed == 0) {
                findings.add(new ValidationFinding(
                        id(),
                        Severity.WARNING,
                        "Event Storm board contains stickies but none have recognised Event Storming types.",
                        "/capture/eventStormBoards/" + board.getId()));
            }
        }
        return findings;
    }
}
