package com.architectureworkbench.api;

import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.discovery.DiscoveryGraphMapper;
import com.architectureworkbench.discovery.DiscoveryService;
import com.architectureworkbench.discovery.HealthcheckService;
import com.architectureworkbench.discovery.LocalRepositoryDiscoveryConnector;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.ProjectionService;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import com.architectureworkbench.reviewboard.ReviewBoardWorkflowService;
import com.architectureworkbench.workspace.InMemoryArchitectureGraphRepository;
import com.architectureworkbench.workspace.InMemoryWorkspaceRepository;
import com.architectureworkbench.workspace.WorkspaceService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KernelConfiguration {
    @Bean
    InMemoryAuditSink kernelAuditSink() {
        return new InMemoryAuditSink();
    }

    @Bean
    ImmutableKnowledgeGraphAuditLog graphAuditLog() {
        return new ImmutableKnowledgeGraphAuditLog();
    }

    @Bean
    ArchitectureElementService architectureElementService(ImmutableKnowledgeGraphAuditLog graphAuditLog) {
        return new ArchitectureElementService(graphAuditLog);
    }

    @Bean
    RelationshipService relationshipService(ImmutableKnowledgeGraphAuditLog graphAuditLog) {
        return new RelationshipService(graphAuditLog);
    }

    @Bean
    ProposedChangeService proposedChangeService(ArchitectureElementService elementService, RelationshipService relationshipService) {
        return new ProposedChangeService(elementService, relationshipService);
    }

    @Bean
    WorkspaceService workspaceService(InMemoryAuditSink kernelAuditSink) {
        return new WorkspaceService(
                new InMemoryWorkspaceRepository(),
                new InMemoryArchitectureGraphRepository(),
                kernelAuditSink
        );
    }

    @Bean
    DiscoveryService discoveryService(
            ArchitectureElementService elementService,
            InMemoryAuditSink kernelAuditSink,
            ProposedChangeService proposedChangeService
    ) {
        return new DiscoveryService(
                List.of(new LocalRepositoryDiscoveryConnector()),
                new DiscoveryGraphMapper(elementService),
                new HealthcheckService(kernelAuditSink),
                kernelAuditSink,
                proposedChangeService
        );
    }

    @Bean
    ReviewBoardWorkflowService reviewBoardWorkflowService(InMemoryAuditSink kernelAuditSink) {
        return new ReviewBoardWorkflowService(kernelAuditSink);
    }

    @Bean
    ProjectionService projectionService(ImmutableKnowledgeGraphAuditLog graphAuditLog) {
        return new ProjectionService(graphAuditLog);
    }
}
