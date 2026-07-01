package com.architectureworkbench.core.graph;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.architecture.ExternalSystem;
import com.architectureworkbench.core.model.architecture.Integration;
import com.architectureworkbench.core.model.architecture.Service;
import com.architectureworkbench.core.model.deployment.DeploymentResource;
import com.architectureworkbench.core.model.domain.Aggregate;
import com.architectureworkbench.core.model.domain.BoundedContext;
import com.architectureworkbench.core.model.domain.Command;
import com.architectureworkbench.core.model.domain.DomainEvent;
import com.architectureworkbench.core.model.domain.Policy;
import com.architectureworkbench.core.model.domain.ReadModel;
import com.architectureworkbench.core.model.governance.AiJudge;
import com.architectureworkbench.core.model.graph.ArchitectureGraph;
import com.architectureworkbench.core.model.graph.GraphEdge;
import com.architectureworkbench.core.model.graph.GraphNode;

/**
 * Deterministically projects an ArchitectureModel into a graph suitable for the React Flow explorer.
 * This is intentionally AI-free: the graph is derived from explicit model relationships.
 */
public class ArchitectureGraphBuilder {

    public ArchitectureGraph build(ArchitectureModel model) {
        ArchitectureGraph graph = new ArchitectureGraph();

        for (BoundedContext context : model.getDomain().getBoundedContexts()) {
            String contextId = id("ctx", context.getName());
            graph.addNode(new GraphNode(contextId, "BOUNDED_CONTEXT", context.getName()));

            for (Aggregate aggregate : context.getAggregates()) {
                String aggregateId = id("agg", aggregate.getName());
                graph.addNode(new GraphNode(aggregateId, "AGGREGATE", aggregate.getName()));
                graph.addEdge(new GraphEdge(contextId, aggregateId, "contains"));
            }

            for (Command command : context.getCommands()) {
                String commandId = id("cmd", command.getName());
                graph.addNode(new GraphNode(commandId, "COMMAND", command.getName()));
                if (present(command.getHandledByAggregate())) {
                    graph.addEdge(new GraphEdge(commandId, id("agg", command.getHandledByAggregate()), "targets"));
                }
            }

            for (DomainEvent event : context.getEvents()) {
                String eventId = id("evt", event.getName());
                graph.addNode(new GraphNode(eventId, "DOMAIN_EVENT", event.getName()));
                if (present(event.getEmittedByAggregate())) {
                    graph.addEdge(new GraphEdge(id("agg", event.getEmittedByAggregate()), eventId, "emits"));
                }
            }

            for (Policy policy : context.getPolicies()) {
                String policyId = id("pol", policy.getName());
                graph.addNode(new GraphNode(policyId, "POLICY", policy.getName()));
                if (present(policy.getTriggerEvent())) {
                    graph.addEdge(new GraphEdge(id("evt", policy.getTriggerEvent()), policyId, "triggers"));
                }
                if (present(policy.getIssuesCommand())) {
                    graph.addEdge(new GraphEdge(policyId, id("cmd", policy.getIssuesCommand()), "issues"));
                }
            }

            for (ReadModel readModel : context.getReadModels()) {
                String readModelId = id("rm", readModel.getName());
                graph.addNode(new GraphNode(readModelId, "READ_MODEL", readModel.getName()));
                for (String eventName : readModel.getPopulatedByEvents()) {
                    graph.addEdge(new GraphEdge(id("evt", eventName), readModelId, "populates"));
                }
            }
        }

        for (Service service : model.getArchitecture().getServices()) {
            String serviceId = id("svc", service.getName());
            graph.addNode(new GraphNode(serviceId, "SERVICE", service.getName()));
            for (String aggregateName : service.getOwnsAggregates()) {
                graph.addEdge(new GraphEdge(serviceId, id("agg", aggregateName), "owns"));
            }
            for (String eventName : service.getPublishesEvents()) {
                graph.addEdge(new GraphEdge(serviceId, id("evt", eventName), "publishes"));
            }
            for (String eventName : service.getSubscribesToEvents()) {
                graph.addEdge(new GraphEdge(id("evt", eventName), serviceId, "subscribed by"));
            }
        }

        for (ExternalSystem externalSystem : model.getArchitecture().getExternalSystems()) {
            graph.addNode(new GraphNode(id("ext", externalSystem.getName()), "EXTERNAL_SYSTEM", externalSystem.getName()));
        }

        for (Integration integration : model.getArchitecture().getIntegrations()) {
            if (present(integration.getSource()) && present(integration.getTarget())) {
                graph.addEdge(new GraphEdge(idForUnknown(integration.getSource()), idForUnknown(integration.getTarget()), integration.getStyle()));
            }
        }

        for (DeploymentResource resource : model.getDeployment().getResources()) {
            String resourceId = id("res", resource.getName());
            graph.addNode(new GraphNode(resourceId, "DEPLOYMENT_RESOURCE", resource.getName()));
            if (present(resource.getOwnerService())) {
                graph.addEdge(new GraphEdge(id("svc", resource.getOwnerService()), resourceId, "uses"));
            }
        }

        for (AiJudge judge : model.getGovernance().getAi().getJudges()) {
            if (judge.isEnabled()) {
                graph.addNode(new GraphNode(id("judge", judge.getId()), "AI_JUDGE", judge.getId()));
            }
        }
        graph.addNode(new GraphNode("audit-log", "AUDIT_LOG", "Immutable Activity Log"));
        for (AiJudge judge : model.getGovernance().getAi().getJudges()) {
            if (judge.isEnabled()) {
                graph.addEdge(new GraphEdge(id("judge", judge.getId()), "audit-log", "records assessment"));
            }
        }

        return graph;
    }

    private static String id(String prefix, String value) {
        return prefix + "-" + slug(value);
    }

    private static String idForUnknown(String value) {
        String slug = slug(value);
        return slug.startsWith("svc-") || slug.startsWith("ext-") ? slug : "node-" + slug;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String slug(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
