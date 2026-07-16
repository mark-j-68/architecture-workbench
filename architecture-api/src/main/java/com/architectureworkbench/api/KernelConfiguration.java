package com.architectureworkbench.api;

import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.discovery.DiscoveryGraphMapper;
import com.architectureworkbench.discovery.DiscoveryPluginPipeline;
import com.architectureworkbench.discovery.DiscoveryService;
import com.architectureworkbench.discovery.HealthcheckService;
import com.architectureworkbench.discovery.LocalRepositoryDiscoveryConnector;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.ProjectionService;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import com.architectureworkbench.reviewboard.ReviewBoardWorkflowService;
import com.architectureworkbench.workspace.ArchitectureGraphRepository;
import com.architectureworkbench.workspace.FileArchitectureGraphRepository;
import com.architectureworkbench.workspace.FileAuditSink;
import com.architectureworkbench.workspace.FileProposedChangeRepository;
import com.architectureworkbench.workspace.FileWorkspaceRepository;
import com.architectureworkbench.workspace.FileWorkspaceStorage;
import com.architectureworkbench.workspace.InMemoryProposedChangeRepository;
import com.architectureworkbench.workspace.InMemoryArchitectureGraphRepository;
import com.architectureworkbench.workspace.InMemoryWorkspaceRepository;
import com.architectureworkbench.workspace.ProposedChangeRepository;
import com.architectureworkbench.workspace.WorkspaceRepository;
import com.architectureworkbench.workspace.WorkspaceService;
import java.nio.file.Path;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
class KernelConfiguration {
    @Bean
    @Primary
    AuditSink kernelAuditSink(Environment environment) {
        if (inMemory(environment)) {
            return new com.architectureworkbench.audit.InMemoryAuditSink();
        }
        return new FileAuditSink(storageRoot(environment));
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
    WorkspaceRepository workspaceRepository(Environment environment) {
        return inMemory(environment) ? new InMemoryWorkspaceRepository() : new FileWorkspaceRepository(storageRoot(environment));
    }

    @Bean
    ArchitectureGraphRepository architectureGraphRepository(Environment environment) {
        return inMemory(environment) ? new InMemoryArchitectureGraphRepository() : new FileArchitectureGraphRepository(storageRoot(environment));
    }

    @Bean
    ProposedChangeRepository proposedChangeRepository(Environment environment) {
        return inMemory(environment) ? new InMemoryProposedChangeRepository() : new FileProposedChangeRepository(storageRoot(environment));
    }

    @Bean
    ReviewBoardSessionStore reviewBoardSessionStore(Environment environment) {
        return inMemory(environment) ? new InMemoryReviewBoardSessionStore() : new FileReviewBoardSessionStore(storageRoot(environment));
    }

    @Bean
    DiscoveryRunReadRepository discoveryRunReadRepository(Environment environment, ObjectMapper objectMapper) {
        return inMemory(environment) ? new InMemoryDiscoveryRunReadRepository()
                : new FileDiscoveryRunReadRepository(storageRoot(environment), objectMapper);
    }

    @Bean
    ProductRepositoryStore productRepositoryStore(Environment environment, ObjectMapper objectMapper) {
        return inMemory(environment) ? new InMemoryProductRepositoryStore() : new FileProductRepositoryStore(storageRoot(environment), objectMapper);
    }

    @Bean
    ProductCompositionService productCompositionService(ProductRepositoryStore products, DiscoveryRunReadRepository runs, AuditSink auditSink) {
        return new ProductCompositionService(products, runs, auditSink);
    }

    @Bean
    DiscoveryPluginPipeline discoveryPluginPipeline() {
        return new DiscoveryPluginPipeline();
    }

    @Bean
    DiscoveryRunExplorerService discoveryRunExplorerService(
            DiscoveryPluginPipeline pipeline,
            DiscoveryRunReadRepository repository,
            AuditSink kernelAuditSink
    ) {
        return new DiscoveryRunExplorerService(pipeline, repository, kernelAuditSink);
    }

    @Bean
    WorkspaceService workspaceService(WorkspaceRepository workspaceRepository, ArchitectureGraphRepository graphRepository, AuditSink kernelAuditSink) {
        return new WorkspaceService(workspaceRepository, graphRepository, kernelAuditSink);
    }

    @Bean
    DiscoveryService discoveryService(
            ArchitectureElementService elementService,
            AuditSink kernelAuditSink,
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
    ReviewBoardWorkflowService reviewBoardWorkflowService(AuditSink kernelAuditSink) {
        return new ReviewBoardWorkflowService(kernelAuditSink);
    }

    @Bean
    ProjectionService projectionService(ImmutableKnowledgeGraphAuditLog graphAuditLog) {
        return new ProjectionService(graphAuditLog);
    }

    private static boolean inMemory(Environment environment) {
        return "in-memory".equalsIgnoreCase(environment.getProperty("architecture.workbench.persistence", "file"));
    }

    private static Path storageRoot(Environment environment) {
        String configured = environment.getProperty(FileWorkspaceStorage.STORAGE_DIR_PROPERTY);
        return configured == null || configured.isBlank() ? FileWorkspaceStorage.defaultRoot() : Path.of(configured);
    }
}
