package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.domain.DomainEvent;
import com.architectureworkbench.core.model.validation.Severity;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventMustHavePublisherRule implements ValidationRule {
    @Override public String id() { return "EVT-001"; }
    @Override public String description() { return "Every domain event must be emitted by a known aggregate."; }

    @Override
    public List<ValidationFinding> validate(ArchitectureModel model) {
        Set<String> aggregates = new HashSet<>();
        model.getDomain().getBoundedContexts().forEach(ctx -> ctx.getAggregates().forEach(a -> aggregates.add(a.getName())));
        List<ValidationFinding> findings = new ArrayList<>();
        model.getDomain().getBoundedContexts().forEach(ctx -> {
            for (DomainEvent event : ctx.getEvents()) {
                if (event.getEmittedByAggregate() == null || !aggregates.contains(event.getEmittedByAggregate())) {
                    findings.add(new ValidationFinding(id(), Severity.ERROR,
                            "Event '%s' is not emitted by a known aggregate.".formatted(event.getName()),
                            "domain.boundedContexts[%s].events[%s]".formatted(ctx.getName(), event.getName())));
                }
            }
        });
        return findings;
    }
}
