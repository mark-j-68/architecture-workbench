package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.validation.ValidationReport;
import java.util.ArrayList;
import java.util.List;

public class ArchitectureValidator {
    private final List<ValidationRule> rules = new ArrayList<>();

    public ArchitectureValidator register(ValidationRule rule) {
        rules.add(rule);
        return this;
    }

    public ValidationReport validate(ArchitectureModel model) {
        ValidationReport report = new ValidationReport();
        rules.stream()
                .flatMap(rule -> rule.validate(model).stream())
                .forEach(report::add);
        return report;
    }
}
