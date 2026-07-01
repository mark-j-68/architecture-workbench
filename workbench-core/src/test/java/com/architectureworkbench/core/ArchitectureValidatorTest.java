package com.architectureworkbench.core;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.domain.Aggregate;
import com.architectureworkbench.core.model.domain.BoundedContext;
import com.architectureworkbench.core.validation.AggregateMustHaveRootEntityRule;
import com.architectureworkbench.core.validation.ArchitectureValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureValidatorTest {
    @Test
    void reportsAggregateWithoutRootEntity() {
        ArchitectureModel model = new ArchitectureModel();
        BoundedContext context = new BoundedContext();
        context.setName("Test Context");
        Aggregate aggregate = new Aggregate();
        aggregate.setName("Application");
        context.getAggregates().add(aggregate);
        model.getDomain().getBoundedContexts().add(context);

        var report = new ArchitectureValidator()
                .register(new AggregateMustHaveRootEntityRule())
                .validate(model);

        assertTrue(report.hasErrors());
    }
}
