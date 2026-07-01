package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.List;

public interface ValidationRule {
    String id();
    String description();
    List<ValidationFinding> validate(ArchitectureModel model);
}
