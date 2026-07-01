package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.CloseReviewBoardSessionRequest;
import com.architectureworkbench.api.ApiDtos.CreateWorkspaceRequest;
import com.architectureworkbench.api.ApiDtos.DecideProposedChangeRequest;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunResponse;
import com.architectureworkbench.api.ApiDtos.FindingResponse;
import com.architectureworkbench.api.ApiDtos.GenerateProjectionRequest;
import com.architectureworkbench.api.ApiDtos.GraphResponse;
import com.architectureworkbench.api.ApiDtos.OpenReviewBoardSessionRequest;
import com.architectureworkbench.api.ApiDtos.ProjectionResponse;
import com.architectureworkbench.api.ApiDtos.ProposedChangeResponse;
import com.architectureworkbench.api.ApiDtos.RecordReviewBoardVoteRequest;
import com.architectureworkbench.api.ApiDtos.RecommendationResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.api.ApiDtos.RunLocalDiscoveryRequest;
import com.architectureworkbench.api.ApiDtos.WorkspaceResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class ArchitectureKernelController {
    private final ArchitectureKernelApiFacade facade;

    ArchitectureKernelController(ArchitectureKernelApiFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    WorkspaceResponse createWorkspace(@RequestBody CreateWorkspaceRequest request) {
        return facade.createWorkspace(request);
    }

    @GetMapping("/workspaces")
    List<WorkspaceResponse> listWorkspaces() {
        return facade.listWorkspaces();
    }

    @GetMapping("/workspaces/{workspaceId}/graph")
    GraphResponse getWorkspaceGraph(@PathVariable("workspaceId") String workspaceId) {
        return facade.getWorkspaceGraph(workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/discovery/local")
    DiscoveryRunResponse runLocalDiscovery(@PathVariable("workspaceId") String workspaceId, @RequestBody RunLocalDiscoveryRequest request) {
        return facade.runLocalDiscovery(workspaceId, request);
    }

    @GetMapping("/workspaces/{workspaceId}/discovery/runs/{runId}/findings")
    List<FindingResponse> listDiscoveryFindings(@PathVariable("workspaceId") String workspaceId, @PathVariable("runId") String runId) {
        return facade.listDiscoveryFindings(workspaceId, runId);
    }

    @GetMapping("/workspaces/{workspaceId}/discovery/runs/{runId}/recommendations")
    List<RecommendationResponse> listDiscoveryRecommendations(@PathVariable("workspaceId") String workspaceId, @PathVariable("runId") String runId) {
        return facade.listDiscoveryRecommendations(workspaceId, runId);
    }

    @GetMapping("/workspaces/{workspaceId}/discovery/runs/{runId}/proposed-changes")
    List<ProposedChangeResponse> listDiscoveryProposedChanges(@PathVariable("workspaceId") String workspaceId, @PathVariable("runId") String runId) {
        return facade.listDiscoveryProposedChanges(workspaceId, runId);
    }

    @PostMapping("/workspaces/{workspaceId}/review-board/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    ReviewBoardSessionResponse openReviewBoardSession(
            @PathVariable("workspaceId") String workspaceId,
            @RequestBody OpenReviewBoardSessionRequest request
    ) {
        return facade.openReviewBoardSession(workspaceId, request);
    }

    @PostMapping("/review-board/sessions/{sessionId}/votes")
    ReviewBoardSessionResponse recordReviewBoardVote(
            @PathVariable("sessionId") String sessionId,
            @RequestBody RecordReviewBoardVoteRequest request
    ) {
        return facade.recordReviewBoardVote(sessionId, request);
    }

    @PostMapping("/review-board/sessions/{sessionId}/close")
    ReviewBoardSessionResponse closeReviewBoardSession(
            @PathVariable("sessionId") String sessionId,
            @RequestBody CloseReviewBoardSessionRequest request
    ) {
        return facade.closeReviewBoardSession(sessionId, request);
    }

    @PostMapping("/proposed-changes/{proposedChangeId}/accept")
    ProposedChangeResponse acceptProposedChange(
            @PathVariable("proposedChangeId") String proposedChangeId,
            @RequestBody DecideProposedChangeRequest request
    ) {
        return facade.acceptProposedChange(proposedChangeId, request);
    }

    @PostMapping("/proposed-changes/{proposedChangeId}/reject")
    ProposedChangeResponse rejectProposedChange(
            @PathVariable("proposedChangeId") String proposedChangeId,
            @RequestBody DecideProposedChangeRequest request
    ) {
        return facade.rejectProposedChange(proposedChangeId, request);
    }

    @PostMapping("/proposed-changes/{proposedChangeId}/defer")
    ProposedChangeResponse deferProposedChange(
            @PathVariable("proposedChangeId") String proposedChangeId,
            @RequestBody DecideProposedChangeRequest request
    ) {
        return facade.deferProposedChange(proposedChangeId, request);
    }

    @PostMapping("/workspaces/{workspaceId}/projections")
    ProjectionResponse generateProjection(@PathVariable("workspaceId") String workspaceId, @RequestBody GenerateProjectionRequest request) {
        return facade.generateProjection(workspaceId, request);
    }
}
