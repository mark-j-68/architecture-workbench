package com.architectureworkbench.knowledgegraph;

public class DddConsistencyValidationService {
    public ValidationReport validate(ArchitectureKnowledgeGraph graph) {
        ValidationReport report = new ValidationReport();

        graph.elementsOfType(ArchitectureElementType.AGGREGATE).forEach(element -> {
            Aggregate aggregate = (Aggregate) element;
            if (aggregate.rootEntity().isBlank()) {
                report.add(new ValidationFinding("KG-DDD-001", ValidationSeverity.ERROR,
                        "Aggregate '%s' must declare a root entity.".formatted(aggregate.name()),
                        aggregate.id().value()));
            }
            if (graph.incoming(aggregate.id(), RelationshipType.CONTAINS).stream()
                    .noneMatch(rel -> graph.element(rel.sourceId()).map(src -> src.type() == ArchitectureElementType.BOUNDED_CONTEXT).orElse(false))) {
                report.add(new ValidationFinding("KG-DDD-002", ValidationSeverity.WARNING,
                        "Aggregate '%s' is not contained by a bounded context.".formatted(aggregate.name()),
                        aggregate.id().value()));
            }
        });

        graph.elementsOfType(ArchitectureElementType.COMMAND).forEach(command -> {
            if (graph.outgoing(command.id(), RelationshipType.HANDLED_BY).isEmpty()) {
                report.add(new ValidationFinding("KG-DDD-003", ValidationSeverity.ERROR,
                        "Command '%s' must be handled by an aggregate.".formatted(command.name()),
                        command.id().value()));
            }
        });

        graph.elementsOfType(ArchitectureElementType.DOMAIN_EVENT).forEach(event -> {
            if (graph.incoming(event.id(), RelationshipType.EMITS).isEmpty()) {
                report.add(new ValidationFinding("KG-DDD-004", ValidationSeverity.WARNING,
                        "DomainEvent '%s' is not emitted by an aggregate.".formatted(event.name()),
                        event.id().value()));
            }
        });

        return report;
    }
}
