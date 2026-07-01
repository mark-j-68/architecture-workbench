package com.architectureworkbench.core;

import com.architectureworkbench.core.graph.ArchitectureGraphBuilder;
import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.domain.Aggregate;
import com.architectureworkbench.core.model.domain.BoundedContext;
import com.architectureworkbench.core.model.domain.Command;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureGraphBuilderTest {
    @Test
    void projectsCommandToAggregateRelationship() {
        ArchitectureModel model = new ArchitectureModel();
        BoundedContext context = new BoundedContext();
        context.setName("Mortgage Origination");
        Aggregate aggregate = new Aggregate();
        aggregate.setName("MortgageApplication");
        aggregate.setRootEntity("MortgageApplication");
        Command command = new Command();
        command.setName("SubmitMortgageApplication");
        command.setHandledByAggregate("MortgageApplication");
        context.getAggregates().add(aggregate);
        context.getCommands().add(command);
        model.getDomain().getBoundedContexts().add(context);

        var graph = new ArchitectureGraphBuilder().build(model);

        assertTrue(graph.getNodes().stream().anyMatch(node -> node.id().equals("cmd-submitmortgageapplication")));
        assertTrue(graph.getEdges().stream().anyMatch(edge -> edge.relationship().equals("targets")));
    }
}
