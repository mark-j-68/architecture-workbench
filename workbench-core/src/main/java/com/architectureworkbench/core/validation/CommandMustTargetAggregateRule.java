package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.domain.Command;
import com.architectureworkbench.core.model.validation.Severity;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.*;

public class CommandMustTargetAggregateRule implements ValidationRule {
    @Override public String id() { return "CMD-001"; }
    @Override public String description() { return "Every command must be handled by a known aggregate."; }

    @Override public List<ValidationFinding> validate(ArchitectureModel model) {
        Set<String> aggregateNames = new HashSet<>();
        model.getDomain().getBoundedContexts().forEach(ctx -> ctx.getAggregates().forEach(a -> aggregateNames.add(a.getName())));
        List<ValidationFinding> findings = new ArrayList<>();
        model.getDomain().getBoundedContexts().forEach(ctx -> {
            for (Command command : ctx.getCommands()) {
                if (command.getHandledByAggregate() == null || !aggregateNames.contains(command.getHandledByAggregate())) {
                    findings.add(new ValidationFinding(id(), Severity.ERROR,
                            "Command '%s' does not target a known aggregate.".formatted(command.getName()),
                            "domain.boundedContexts[%s].commands[%s]".formatted(ctx.getName(), command.getName())));
                }
            }
        });
        return findings;
    }
}
