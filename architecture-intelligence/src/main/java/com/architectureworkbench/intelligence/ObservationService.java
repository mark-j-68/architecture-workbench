package com.architectureworkbench.intelligence;

import java.util.ArrayList;
import java.util.List;

public class ObservationService {
    public Observation attachEvidence(Observation observation, Evidence evidence) {
        List<Evidence> evidenceList = new ArrayList<>(observation.relatedEvidence());
        evidenceList.add(evidence);
        return new Observation(
                observation.id(),
                observation.source(),
                observation.description(),
                evidenceList,
                observation.relatedGraphElements()
        );
    }
}
