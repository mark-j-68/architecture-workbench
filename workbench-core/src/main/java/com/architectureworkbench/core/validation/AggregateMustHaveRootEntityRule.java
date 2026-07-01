package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.domain.Aggregate;
import com.architectureworkbench.core.model.validation.Severity;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.ArrayList;
import java.util.List;

public class AggregateMustHaveRootEntityRule implements ValidationRule {
    @Override public String id() { return "DDD-001"; }
    @Override public String description() { return "Every aggregate must declare a root entity."; }

    @Override
    public List<ValidationFinding> validate(ArchitectureModel model) {
        List<ValidationFinding> findings = new ArrayList<>();
        model.getDomain().getBoundedContexts().forEach(ctx -> {
            for (Aggregate aggregate : ctx.getAggregates()) {
                if (aggregate.getRootEntity() == null || aggregate.getRootEntity().isBlank()) {
                    findings.add(new ValidationFinding(id(), Severity.ERROR,
                            "Aggregate '%s' has no root entity.".formatted(aggregate.getName()),
                            "domain.boundedContexts[%s].aggregates[%s]".formatted(ctx.getName(), aggregate.getName())));
                }
            }
        });
        return findings;
    }
}
