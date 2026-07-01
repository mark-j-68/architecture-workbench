package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.CloseReviewBoardSessionRequest;
import com.architectureworkbench.api.ApiDtos.CreateWorkspaceRequest;
import com.architectureworkbench.api.ApiDtos.DecideProposedChangeRequest;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunResponse;
import com.architectureworkbench.api.ApiDtos.ElementResponse;
import com.architectureworkbench.api.ApiDtos.FindingResponse;
import com.architectureworkbench.api.ApiDtos.GenerateProjectionRequest;
import com.architectureworkbench.api.ApiDtos.GraphResponse;
import com.architectureworkbench.api.ApiDtos.OpenReviewBoardSessionRequest;
import com.architectureworkbench.api.ApiDtos.ProjectionResponse;
import com.architectureworkbench.api.ApiDtos.ProposedChangeResponse;
import com.architectureworkbench.api.ApiDtos.RecordReviewBoardVoteRequest;
import com.architectureworkbench.api.ApiDtos.RecommendationResponse;
import com.architectureworkbench.api.ApiDtos.RelationshipResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardDecisionResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardParticipantRequest;
import com.architectureworkbench.api.ApiDtos.ReviewBoardParticipantResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardVoteResponse;
import com.architectureworkbench.api.ApiDtos.RunLocalDiscoveryRequest;
import com.architectureworkbench.api.ApiDtos.WorkspaceResponse;
import com.architectureworkbench.discovery.DiscoveryContext;
import com.architectureworkbench.discovery.DiscoveryRun;
import com.architectureworkbench.discovery.DiscoveryRunId;
import com.architectureworkbench.discovery.DiscoveryService;
import com.architectureworkbench.discovery.DiscoverySource;
import com.architectureworkbench.discovery.DiscoverySourceType;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.Projection;
import com.architectureworkbench.knowledgegraph.ProjectionService;
import com.architectureworkbench.knowledgegraph.ProjectionType;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.ProposedElementAddition;
import com.architectureworkbench.knowledgegraph.ProposedGraphMutation;
import com.architectureworkbench.knowledgegraph.ProposedRelationshipAddition;
import com.architectureworkbench.knowledgegraph.Relationship;
import com.architectureworkbench.reviewboard.ReviewBoardParticipant;
import com.architectureworkbench.reviewboard.ReviewBoardParticipantType;
import com.architectureworkbench.reviewboard.ReviewBoardSession;
import com.architectureworkbench.reviewboard.ReviewBoardVote;
import com.architectureworkbench.reviewboard.ReviewBoardVoteType;
import com.architectureworkbench.reviewboard.ReviewBoardWorkflowService;
import com.architectureworkbench.workspace.Workspace;
import com.architectureworkbench.workspace.WorkspaceId;
import com.architectureworkbench.workspace.WorkspaceMetadata;
import com.architectureworkbench.workspace.ProposedChangeRepository;
import com.architectureworkbench.workspace.WorkspaceService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
class ArchitectureKernelApiFacade {
    private final WorkspaceService workspaceService;
    private final DiscoveryService discoveryService;
    private final ReviewBoardWorkflowService reviewBoardWorkflowService;
    private final ProposedChangeService proposedChangeService;
    private final ProposedChangeRepository proposedChangeRepository;
    private final ReviewBoardSessionStore reviewBoardSessionStore;
    private final ProjectionService projectionService;
    private final Map<String, DiscoveryRun> discoveryRuns = new ConcurrentHashMap<>();
    private final Map<String, Recommendation> recommendations = new ConcurrentHashMap<>();
    private final Map<String, ProposedArchitectureChange> proposedChanges = new ConcurrentHashMap<>();
    private final Map<String, ReviewBoardSession> reviewBoardSessions = new ConcurrentHashMap<>();
    private final Map<String, WorkspaceId> workspaceIdsByGraphId = new ConcurrentHashMap<>();

    ArchitectureKernelApiFacade(
            WorkspaceService workspaceService,
            DiscoveryService discoveryService,
            ReviewBoardWorkflowService reviewBoardWorkflowService,
            ProposedChangeService proposedChangeService,
            ProposedChangeRepository proposedChangeRepository,
            ReviewBoardSessionStore reviewBoardSessionStore,
            ProjectionService projectionService
    ) {
        this.workspaceService = workspaceService;
        this.discoveryService = discoveryService;
        this.reviewBoardWorkflowService = reviewBoardWorkflowService;
        this.proposedChangeService = proposedChangeService;
        this.proposedChangeRepository = proposedChangeRepository;
        this.reviewBoardSessionStore = reviewBoardSessionStore;
        this.projectionService = projectionService;
    }

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        Workspace workspace = workspaceService.createWorkspace(
                requireText(request.name(), "name"),
                WorkspaceMetadata.empty(),
                actor(request.actorRef())
        );
        ArchitectureKnowledgeGraph graph = workspaceService.getWorkspaceGraph(workspace.id());
        workspaceIdsByGraphId.put(graph.graphId(), workspace.id());
        return workspaceResponse(workspace, graph);
    }

    List<WorkspaceResponse> listWorkspaces() {
        return workspaceService.listWorkspaces().stream()
                .map(workspace -> workspaceResponse(workspace, workspaceService.getWorkspaceGraph(workspace.id())))
                .toList();
    }

    GraphResponse getWorkspaceGraph(String workspaceId) {
        return graphResponse(graph(workspaceId));
    }

    DiscoveryRunResponse runLocalDiscovery(String workspaceId, RunLocalDiscoveryRequest request) {
        ArchitectureKnowledgeGraph graph = graph(workspaceId);
        DiscoveryRun run = discoveryService.runDiscovery(new DiscoveryContext(
                DiscoveryRunId.newId(),
                new DiscoverySource("local-api", DiscoverySourceType.LOCAL_REPOSITORY, requireText(request.path(), "path"), "Local API Source"),
                Path.of(request.path()),
                graph,
                actor(request.actorRef())
        ));
        discoveryRuns.put(run.runId().value(), run);
        run.recommendations().forEach(recommendation -> recommendations.put(recommendation.id(), recommendation));
        run.proposedChanges().forEach(change -> {
            proposedChanges.put(change.id().value(), change);
            proposedChangeRepository.save(change);
        });
        return discoveryRunResponse(workspaceId, graph.graphId(), run);
    }

    List<FindingResponse> listDiscoveryFindings(String workspaceId, String runId) {
        requireGraphMatchesWorkspace(workspaceId, run(runId));
        return run(runId).aimFindings().stream().map(ArchitectureKernelApiFacade::findingResponse).toList();
    }

    List<RecommendationResponse> listDiscoveryRecommendations(String workspaceId, String runId) {
        requireGraphMatchesWorkspace(workspaceId, run(runId));
        return run(runId).recommendations().stream().map(ArchitectureKernelApiFacade::recommendationResponse).toList();
    }

    List<ProposedChangeResponse> listDiscoveryProposedChanges(String workspaceId, String runId) {
        requireGraphMatchesWorkspace(workspaceId, run(runId));
        return run(runId).proposedChanges().stream().map(ArchitectureKernelApiFacade::proposedChangeResponse).toList();
    }

    ReviewBoardSessionResponse openReviewBoardSession(String workspaceId, OpenReviewBoardSessionRequest request) {
        ArchitectureKnowledgeGraph graph = graph(workspaceId);
        List<Recommendation> selectedRecommendations = ids(request.recommendationIds()).stream()
                .map(this::recommendation)
                .toList();
        List<ProposedArchitectureChange> selectedChanges = ids(request.proposedChangeIds()).stream()
                .map(this::proposedChange)
                .toList();
        ReviewBoardSession session = reviewBoardWorkflowService.openSession(
                graph.graphId(),
                selectedChanges.isEmpty()
                        ? com.architectureworkbench.audit.CorrelationId.newId("review-board")
                        : selectedChanges.get(0).correlationId(),
                selectedRecommendations,
                selectedChanges,
                actor(request.actorRef())
        );
        for (ReviewBoardParticipantRequest participant : request.participants() == null ? List.<ReviewBoardParticipantRequest>of() : request.participants()) {
            session = reviewBoardWorkflowService.addParticipant(session, new ReviewBoardParticipant(
                    requireText(participant.participantId(), "participantId"),
                    requireText(participant.name(), "name"),
                    ReviewBoardParticipantType.valueOf(requireText(participant.participantType(), "participantType"))
            ));
        }
        reviewBoardSessions.put(session.sessionId().value(), session);
        return persistReviewBoardSession(WorkspaceId.of(workspaceId), session);
    }

    ReviewBoardSessionResponse recordReviewBoardVote(String sessionId, RecordReviewBoardVoteRequest request) {
        ReviewBoardSession session = session(sessionId);
        session = reviewBoardWorkflowService.recordVote(session, new ReviewBoardVote(
                null,
                requireText(request.participantId(), "participantId"),
                ReviewBoardVoteType.valueOf(requireText(request.voteType(), "voteType")),
                request.rationale(),
                null
        ));
        reviewBoardSessions.put(session.sessionId().value(), session);
        return persistReviewBoardSession(workspaceIdForSession(session), session);
    }

    ReviewBoardSessionResponse closeReviewBoardSession(String sessionId, CloseReviewBoardSessionRequest request) {
        ReviewBoardSession session = reviewBoardWorkflowService.closeSession(session(sessionId), actor(request.actorRef()));
        reviewBoardSessions.put(session.sessionId().value(), session);
        return persistReviewBoardSession(workspaceIdForSession(session), session);
    }

    ProposedChangeResponse acceptProposedChange(String proposedChangeId, DecideProposedChangeRequest request) {
        ProposedArchitectureChange change = proposedChange(proposedChangeId);
        WorkspaceId workspaceId = workspaceIdForChange(change, request.workspaceId());
        ArchitectureKnowledgeGraph graph = workspaceService.getWorkspaceGraph(workspaceId);
        ProposedArchitectureChange accepted = proposedChangeService.acceptProposedChange(
                graph,
                change,
                actor(request.actorRef()),
                request.rationale()
        );
        proposedChanges.put(accepted.id().value(), accepted);
        proposedChangeRepository.save(accepted);
        workspaceService.saveWorkspaceGraph(workspaceId, graph, actor(request.actorRef()));
        return proposedChangeResponse(accepted);
    }

    ProposedChangeResponse rejectProposedChange(String proposedChangeId, DecideProposedChangeRequest request) {
        ProposedArchitectureChange rejected = proposedChangeService.rejectProposedChange(
                proposedChange(proposedChangeId),
                request.rationale()
        );
        proposedChanges.put(rejected.id().value(), rejected);
        proposedChangeRepository.save(rejected);
        return proposedChangeResponse(rejected);
    }

    ProposedChangeResponse deferProposedChange(String proposedChangeId, DecideProposedChangeRequest request) {
        ProposedArchitectureChange deferred = proposedChangeService.deferProposedChange(
                proposedChange(proposedChangeId),
                request.rationale()
        );
        proposedChanges.put(deferred.id().value(), deferred);
        proposedChangeRepository.save(deferred);
        return proposedChangeResponse(deferred);
    }

    ProjectionResponse generateProjection(String workspaceId, GenerateProjectionRequest request) {
        Projection projection = projectionService.generateProjection(
                graph(workspaceId),
                ProjectionType.valueOf(requireText(request.type(), "type")),
                actor(request.actorRef())
        );
        return new ProjectionResponse(
                projection.type().name(),
                projection.generatedAt(),
                projection.sourceElementRefs(),
                projection.sourceRelationshipRefs(),
                projection.payload()
        );
    }

    private ArchitectureKnowledgeGraph graph(String workspaceId) {
        ArchitectureKnowledgeGraph graph = workspaceService.getWorkspaceGraph(WorkspaceId.of(workspaceId));
        workspaceIdsByGraphId.put(graph.graphId(), WorkspaceId.of(workspaceId));
        return graph;
    }

    private DiscoveryRun run(String runId) {
        DiscoveryRun run = discoveryRuns.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Discovery run does not exist: " + runId);
        }
        return run;
    }

    private Recommendation recommendation(String recommendationId) {
        Recommendation recommendation = recommendations.get(recommendationId);
        if (recommendation == null) {
            throw new IllegalArgumentException("Recommendation does not exist: " + recommendationId);
        }
        return recommendation;
    }

    private ProposedArchitectureChange proposedChange(String proposedChangeId) {
        ProposedArchitectureChange change = proposedChanges.get(proposedChangeId);
        if (change == null) {
            change = proposedChangeRepository.findById(proposedChangeId)
                    .orElseThrow(() -> new IllegalArgumentException("Proposed change does not exist: " + proposedChangeId));
            proposedChanges.put(change.id().value(), change);
        }
        return change;
    }

    private ReviewBoardSession session(String sessionId) {
        ReviewBoardSession session = reviewBoardSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Review board session does not exist: " + sessionId);
        }
        return session;
    }

    private WorkspaceId workspaceIdForChange(ProposedArchitectureChange change, String requestedWorkspaceId) {
        if (requestedWorkspaceId != null && !requestedWorkspaceId.isBlank()) {
            return WorkspaceId.of(requestedWorkspaceId);
        }
        WorkspaceId workspaceId = workspaceIdsByGraphId.get(change.workspaceId());
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace id is required for proposed change: " + change.id().value());
        }
        return workspaceId;
    }

    private WorkspaceId workspaceIdForSession(ReviewBoardSession session) {
        return workspaceIdsByGraphId.getOrDefault(session.workspaceId(), WorkspaceId.of(session.workspaceId()));
    }

    private ReviewBoardSessionResponse persistReviewBoardSession(WorkspaceId workspaceId, ReviewBoardSession session) {
        return reviewBoardSessionStore.save(workspaceId, reviewBoardSessionResponse(session));
    }

    private void requireGraphMatchesWorkspace(String workspaceId, DiscoveryRun run) {
        String graphId = graph(workspaceId).graphId();
        if (!run.proposedChanges().isEmpty() && !run.proposedChanges().get(0).workspaceId().equals(graphId)) {
            throw new IllegalArgumentException("Discovery run does not belong to workspace: " + workspaceId);
        }
    }

    private static WorkspaceResponse workspaceResponse(Workspace workspace, ArchitectureKnowledgeGraph graph) {
        return new WorkspaceResponse(workspace.id().value(), workspace.name(), graph.graphId());
    }

    private static GraphResponse graphResponse(ArchitectureKnowledgeGraph graph) {
        return new GraphResponse(
                graph.graphId(),
                graph.elements().stream().map(ArchitectureKernelApiFacade::elementResponse).toList(),
                graph.relationships().stream().map(ArchitectureKernelApiFacade::relationshipResponse).toList()
        );
    }

    private static ElementResponse elementResponse(ArchitectureElement element) {
        return new ElementResponse(
                element.id().value(),
                element.type().name(),
                element.name(),
                element.description(),
                element.attributes()
        );
    }

    private static RelationshipResponse relationshipResponse(Relationship relationship) {
        return new RelationshipResponse(
                relationship.id(),
                relationship.sourceId().value(),
                relationship.targetId().value(),
                relationship.type().name(),
                relationship.label(),
                relationship.attributes()
        );
    }

    private static DiscoveryRunResponse discoveryRunResponse(String workspaceId, String graphId, DiscoveryRun run) {
        return new DiscoveryRunResponse(
                run.runId().value(),
                workspaceId,
                graphId,
                run.startedAt(),
                run.completedAt(),
                run.artifacts().size(),
                run.aimFindings().size(),
                run.recommendations().size(),
                run.proposedChanges().size()
        );
    }

    private static FindingResponse findingResponse(Finding finding) {
        return new FindingResponse(
                finding.id(),
                finding.severity().name(),
                finding.category(),
                finding.description(),
                finding.confidence(),
                finding.supportingObservations().stream()
                        .flatMap(observation -> observation.relatedEvidence().stream())
                        .map(com.architectureworkbench.intelligence.Evidence::id)
                        .distinct()
                        .toList()
        );
    }

    private static RecommendationResponse recommendationResponse(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.id(),
                recommendation.description(),
                recommendation.rationale(),
                recommendation.estimatedImpact(),
                recommendation.estimatedEffort(),
                recommendation.confidence(),
                recommendation.lifecycleStatus().name(),
                recommendation.supportingFindings().stream().map(Finding::id).toList()
        );
    }

    private static ProposedChangeResponse proposedChangeResponse(ProposedArchitectureChange change) {
        return new ProposedChangeResponse(
                change.id().value(),
                change.type().name(),
                change.status().name(),
                change.workspaceId(),
                change.correlationId().value(),
                change.recommendationId(),
                change.findingIds(),
                change.evidenceIds(),
                mutationResponse(change.mutation())
        );
    }

    private static Map<String, String> mutationResponse(ProposedGraphMutation mutation) {
        Map<String, String> values = new LinkedHashMap<>();
        if (mutation instanceof ProposedElementAddition elementAddition) {
            values.put("elementType", elementAddition.elementType().name());
            values.put("name", elementAddition.name());
            values.put("description", elementAddition.description());
            values.putAll(prefix("attribute.", elementAddition.attributes()));
        } else if (mutation instanceof ProposedRelationshipAddition relationshipAddition) {
            values.put("sourceId", relationshipAddition.sourceId().value());
            values.put("targetId", relationshipAddition.targetId().value());
            values.put("relationshipType", relationshipAddition.relationshipType().name());
            values.put("label", relationshipAddition.label());
            values.putAll(prefix("attribute.", relationshipAddition.attributes()));
        }
        return Map.copyOf(values);
    }

    private static ReviewBoardSessionResponse reviewBoardSessionResponse(ReviewBoardSession session) {
        return new ReviewBoardSessionResponse(
                session.sessionId().value(),
                session.workspaceId(),
                session.correlationId().value(),
                session.status().name(),
                session.recommendationCandidates().stream().map(Recommendation::id).toList(),
                session.proposedChanges().stream().map(change -> change.id().value()).toList(),
                session.participants().stream().map(ArchitectureKernelApiFacade::participantResponse).toList(),
                session.votes().stream().map(ArchitectureKernelApiFacade::voteResponse).toList(),
                session.decision() == null ? null : decisionResponse(session.decision())
        );
    }

    private static ReviewBoardParticipantResponse participantResponse(ReviewBoardParticipant participant) {
        return new ReviewBoardParticipantResponse(
                participant.participantId(),
                participant.name(),
                participant.participantType().name()
        );
    }

    private static ReviewBoardVoteResponse voteResponse(ReviewBoardVote vote) {
        return new ReviewBoardVoteResponse(vote.voteId(), vote.participantId(), vote.voteType().name(), vote.rationale(), vote.votedAt());
    }

    private static ReviewBoardDecisionResponse decisionResponse(com.architectureworkbench.reviewboard.ReviewBoardDecision decision) {
        return new ReviewBoardDecisionResponse(
                decision.decisionType().name(),
                decision.rationale(),
                decision.conditions(),
                decision.decidedAt()
        );
    }

    private static List<String> ids(List<String> ids) {
        return ids == null ? List.of() : new ArrayList<>(ids);
    }

    private static Map<String, String> prefix(String prefix, Map<String, String> values) {
        Map<String, String> prefixed = new LinkedHashMap<>();
        values.forEach((key, value) -> prefixed.put(prefix + key, value));
        return prefixed;
    }

    private static String actor(String actorRef) {
        return actorRef == null || actorRef.isBlank() ? "api-user" : actorRef;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
