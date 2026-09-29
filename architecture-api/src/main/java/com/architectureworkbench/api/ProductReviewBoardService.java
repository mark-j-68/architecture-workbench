package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.*;
import com.architectureworkbench.audit.*;
import com.architectureworkbench.intelligence.*;
import com.architectureworkbench.knowledgegraph.*;
import com.architectureworkbench.reviewboard.*;
import com.architectureworkbench.workspace.*;
import java.time.Instant;
import java.util.*;

/** Adapts retained Product recommendations to the existing Review Board and graph proposal workflows. */
final class ProductReviewBoardService {
    private final ProductRepositoryStore products;
    private final ProductArchitectureRecommendationService recommendations;
    private final ReviewBoardWorkflowService reviews;
    private final ReviewBoardSessionStore reviewStore;
    private final ProposedChangeService proposedChanges;
    private final ProposedChangeRepository proposedStore;
    private final WorkspaceService workspaces;
    private final AuditSink audit;

    ProductReviewBoardService(ProductRepositoryStore products, ProductArchitectureRecommendationService recommendations,
                              ReviewBoardWorkflowService reviews, ReviewBoardSessionStore reviewStore,
                              ProposedChangeService proposedChanges, ProposedChangeRepository proposedStore,
                              WorkspaceService workspaces, AuditSink audit) {
        this.products = products; this.recommendations = recommendations; this.reviews = reviews; this.reviewStore = reviewStore;
        this.proposedChanges = proposedChanges; this.proposedStore = proposedStore; this.workspaces = workspaces;
        this.audit = audit;
    }

    ProductReviewSnapshot submit(String w, String p, String id, SubmitProductReviewRequest request) {
        var recommendation = recommendations.get(w, p, id);
        if (!recommendation.status().equals("CANDIDATE")) throw new IllegalStateException("Only a candidate can be submitted for review.");
        if (request == null || request.participants() == null || request.participants().isEmpty())
            throw new IllegalArgumentException("At least one Review Board participant is required.");
        var analysis = analysis(w, p, recommendation.analysisId());
        var graph = workspaces.getWorkspaceGraph(WorkspaceId.of(w));
        var session = reviews.openSession(graph.graphId(), new CorrelationId(analysis.correlationId()),
                List.of(adapt(recommendation, analysis)), List.of(), actor(request.actorRef()));
        for (var participant : request.participants()) session = reviews.addParticipant(session, new ReviewBoardParticipant(
                required(participant.participantId(), "participantId"), required(participant.name(), "name"),
                ReviewBoardParticipantType.valueOf(required(participant.participantType(), "participantType"))));
        var snapshot = save(w, p, recommendation, session, "SUBMITTED", "");
        recommendations.linkReview(w, p, id, session.sessionId().value(), new RecommendationActionRequest(request.actorRef(), request.rationale()));
        return snapshot;
    }

    ProductReviewSnapshot vote(String w, String p, String id, RecordReviewBoardVoteRequest request) {
        var previous = latest(w, p, id);
        var recommendation = recommendations.get(w, p, id);
        if (!Set.of("SUBMITTED", "UNDER_REVIEW").contains(recommendation.status())) throw new IllegalStateException("Review is not open.");
        var session = reviews.recordVote(rehydrate(previous), new ReviewBoardVote(null,
                required(request.participantId(), "participantId"), ReviewBoardVoteType.valueOf(required(request.voteType(), "voteType")),
                request.rationale(), null));
        var snapshot = save(w, p, recommendation, session, "UNDER_REVIEW", previous.proposedChangeId());
        recommendations.transition(w, p, id, "UNDER_REVIEW", new RecommendationActionRequest(request.participantId(), request.rationale()));
        return snapshot;
    }

    ProductReviewSnapshot close(String w, String p, String id, CloseReviewBoardSessionRequest request) {
        var previous = latest(w, p, id);
        var recommendation = recommendations.get(w, p, id);
        if (!Set.of("SUBMITTED", "UNDER_REVIEW").contains(recommendation.status())) throw new IllegalStateException("Review is not open.");
        var session = reviews.closeSession(rehydrate(previous), actor(request.actorRef()));
        String state = switch (session.decision().decisionType()) {
            case ACCEPT_PROPOSED_CHANGE -> "APPROVED";
            case REJECT_PROPOSED_CHANGE -> "REJECTED";
            case DEFER_PROPOSED_CHANGE -> "DEFERRED";
            default -> "UNDER_REVIEW";
        };
        var snapshot = save(w, p, recommendation, session, state, previous.proposedChangeId());
        recommendations.transition(w, p, id, state, new RecommendationActionRequest(request.actorRef(), session.decision().rationale()));
        audit(w, p, id, ArchitectureEventType.PRODUCT_RECOMMENDATION_REVIEW_DECIDED,
                Map.of("reviewSessionId", session.sessionId().value(), "decision", session.decision().decisionType().name()));
        return snapshot;
    }

    ProposedChangeResponse propose(String w, String p, String id, ProductProposedElementRequest request) {
        var recommendation = recommendations.get(w, p, id);
        var previous = latest(w, p, id);
        if (!recommendation.status().equals("APPROVED") || !previous.state().equals("APPROVED"))
            throw new IllegalStateException("An approved Review Board decision is required before proposing a graph change.");
        if (recommendation.supportingFindingIds().isEmpty() || recommendation.evidenceIds().isEmpty())
            throw new IllegalStateException("A graph proposal requires source findings and evidence.");
        if (request == null) throw new IllegalArgumentException("Explicit graph mutation details are required.");
        ArchitectureElementType type = ArchitectureElementType.valueOf(required(request.elementType(), "elementType"));
        if (type == ArchitectureElementType.EVIDENCE || type == ArchitectureElementType.ARCHITECTURE_REVIEW)
            throw new IllegalArgumentException("Choose a concrete architecture element type.");
        var graph = workspaces.getWorkspaceGraph(WorkspaceId.of(w));
        var attributes = Map.of("productId", p, "analysisId", recommendation.analysisId(),
                "reviewSessionId", previous.session().sessionId(), "recommendationId", id,
                "repositoryIds", String.join(",", previous.repositoryIds()),
                "discoveryRunIds", String.join(",", previous.discoveryRunIds()));
        var change = proposedChanges.proposeElementAddition(graph.graphId(), new CorrelationId(previous.session().correlationId()),
                new ProposedElementAddition(type, required(request.name(), "name"), request.description(), attributes),
                id, recommendation.supportingFindingIds(), recommendation.evidenceIds());
        proposedStore.save(change);
        recommendations.linkProposedChange(w, p, id, change.id().value(), new RecommendationActionRequest(request.actorRef(), request.rationale()));
        save(w, p, recommendation, rehydrate(previous), "PROPOSED_CHANGE", change.id().value());
        return response(change);
    }

    ProposedChangeResponse accept(String w, String p, String id, DecideProposedChangeRequest request) {
        var recommendation = recommendations.get(w, p, id);
        if (!recommendation.status().equals("PROPOSED_CHANGE") || recommendation.proposedChangeId().isBlank())
            throw new IllegalStateException("An explicit proposed change is required.");
        var change = proposedStore.findById(recommendation.proposedChangeId())
                .orElseThrow(() -> new NoSuchElementException("Proposed change not found: " + recommendation.proposedChangeId()));
        var graph = workspaces.getWorkspaceGraph(WorkspaceId.of(w));
        var acceptedChange = proposedChanges.acceptProposedChange(graph, change, actor(request.actorRef()), request.rationale());
        proposedStore.save(acceptedChange);
        workspaces.saveWorkspaceGraph(WorkspaceId.of(w), graph, actor(request.actorRef()));
        var accepted = response(acceptedChange);
        var previous = latest(w, p, id);
        recommendations.transition(w, p, id, "ARCHITECTURE_CHANGE_ACCEPTED", new RecommendationActionRequest(request.actorRef(), request.rationale()));
        save(w, p, recommendation, rehydrate(previous), "ARCHITECTURE_CHANGE_ACCEPTED", accepted.id());
        audit(w, p, id, ArchitectureEventType.PRODUCT_RECOMMENDATION_ARCHITECTURE_CHANGE_ACCEPTED,
                Map.of("reviewSessionId", previous.session().sessionId(), "proposedChangeId", accepted.id()));
        return accepted;
    }

    ProductReviewSnapshot latest(String w, String p, String id) {
        return history(w, p, id).stream().reduce((a, b) -> b)
                .orElseThrow(() -> new NoSuchElementException("Review not found for recommendation: " + id));
    }

    List<ProductReviewSnapshot> history(String w, String p, String id) {
        recommendations.get(w, p, id);
        return products.reviewSnapshots(w, p).stream().filter(s -> s.recommendationId().equals(id)).toList();
    }

    ProposedChangeResponse proposedChange(String w, String p, String id) {
        String changeId = recommendations.get(w, p, id).proposedChangeId();
        if (changeId.isBlank()) throw new NoSuchElementException("No proposed change for recommendation: " + id);
        return response(proposedStore.findById(changeId).orElseThrow(() -> new NoSuchElementException("Proposed change not found: " + changeId)));
    }

    private ProductReviewSnapshot save(String w, String p, ProductArchitectureRecommendationView recommendation,
                                       ReviewBoardSession session, String state, String proposedChangeId) {
        var response = response(session);
        var product = products.find(w, p).orElseThrow();
        var repositories = recommendation.repositoryIds().isEmpty()
                ? product.repositories() : product.repositories().stream().filter(r -> recommendation.repositoryIds().contains(r.id().value())).toList();
        var snapshot = new ProductReviewSnapshot("product-review-" + UUID.randomUUID(), w, p,
                recommendation.recommendationId(), recommendation.analysisId(), recommendation.compositionVersion(),
                repositories.stream().map(r -> r.id().value()).toList(),
                repositories.stream().flatMap(r -> r.discoveryRunIds().stream()).distinct().toList(),
                recommendation.supportingFindingIds(), recommendation.evidenceIds(), state, response, proposedChangeId, Instant.now());
        products.saveReviewSnapshot(w, p, snapshot);
        reviewStore.save(WorkspaceId.of(w), response);
        return snapshot;
    }

    private ReviewBoardSession rehydrate(ProductReviewSnapshot snapshot) {
        var response = snapshot.session();
        var recommendation = recommendations.get(snapshot.workspaceId(), snapshot.productId(), snapshot.recommendationId());
        var analysis = analysis(snapshot.workspaceId(), snapshot.productId(), snapshot.analysisId());
        var participants = response.participants().stream().map(x -> new ReviewBoardParticipant(x.participantId(), x.name(),
                ReviewBoardParticipantType.valueOf(x.participantType()))).toList();
        var votes = response.votes().stream().map(x -> new ReviewBoardVote(x.voteId(), x.participantId(),
                ReviewBoardVoteType.valueOf(x.voteType()), x.rationale(), x.votedAt())).toList();
        var decision = response.decision() == null ? null : new ReviewBoardDecision(
                ReviewBoardDecisionType.valueOf(response.decision().decisionType()), response.decision().rationale(),
                response.decision().conditions(), response.decision().decidedAt());
        return new ReviewBoardSession(new ReviewBoardSessionId(response.sessionId()), response.workspaceId(),
                new CorrelationId(response.correlationId()), ReviewBoardSessionStatus.valueOf(response.status()),
                List.of(adapt(recommendation, analysis)), List.of(), participants, votes, decision, null, null);
    }

    private static ReviewBoardSessionResponse response(ReviewBoardSession session) {
        return new ReviewBoardSessionResponse(session.sessionId().value(), session.workspaceId(), session.correlationId().value(),
                session.status().name(), session.recommendationCandidates().stream().map(Recommendation::id).toList(),
                List.of(), session.participants().stream().map(x -> new ReviewBoardParticipantResponse(x.participantId(), x.name(), x.participantType().name())).toList(),
                session.votes().stream().map(x -> new ReviewBoardVoteResponse(x.voteId(), x.participantId(), x.voteType().name(), x.rationale(), x.votedAt())).toList(),
                session.decision() == null ? null : new ReviewBoardDecisionResponse(session.decision().decisionType().name(),
                        session.decision().rationale(), session.decision().conditions(), session.decision().decidedAt()));
    }

    private static ProposedChangeResponse response(ProposedArchitectureChange change) {
        Map<String,String> mutation = new LinkedHashMap<>();
        if (change.mutation() instanceof ProposedElementAddition element) {
            mutation.put("elementType", element.elementType().name());
            mutation.put("name", element.name());
            mutation.put("description", element.description());
            element.attributes().forEach((key, value) -> mutation.put("attribute." + key, value));
        } else if (change.mutation() instanceof ProposedRelationshipAddition relationship) {
            mutation.put("sourceId", relationship.sourceId().value());
            mutation.put("targetId", relationship.targetId().value());
            mutation.put("relationshipType", relationship.relationshipType().name());
            mutation.put("label", relationship.label());
            relationship.attributes().forEach((key, value) -> mutation.put("attribute." + key, value));
        }
        return new ProposedChangeResponse(change.id().value(), change.type().name(), change.status().name(),
                change.workspaceId(), change.correlationId().value(), change.recommendationId(),
                change.findingIds(), change.evidenceIds(), Map.copyOf(mutation));
    }

    private static Recommendation adapt(ProductArchitectureRecommendationView recommendation, ProductArchitectureAnalysisView analysis) {
        var findings = analysis.findings().stream().filter(f -> recommendation.supportingFindingIds().contains(f.findingId())).map(f -> {
            var evidenceIds = f.supportingEvidenceIds().isEmpty() ? List.of("analysis-gap-" + analysis.analysisId()) : f.supportingEvidenceIds();
            var evidence = evidenceIds.stream().map(id -> new com.architectureworkbench.intelligence.Evidence(id,
                    id.startsWith("analysis-gap-") ? "analysis-missing-evidence" : "product-discovery",
                    f.derivationSummary(), f.confidenceScore(), f.generatedAt(),
                    List.of(analysis.workspaceId(), analysis.productId(), analysis.analysisId()), List.of())).toList();
            var observation = new Observation("product-observation-" + f.findingId(), "product-analysis", f.derivationSummary(), evidence, List.of());
            Severity severity = switch (f.severity()) { case "CRITICAL" -> Severity.CRITICAL; case "HIGH" -> Severity.ERROR; case "MEDIUM" -> Severity.WARNING; default -> Severity.INFO; };
            return new Finding(f.findingId(), severity, f.findingType(), f.description(), List.of(observation), f.confidenceScore());
        }).toList();
        if (findings.isEmpty()) {
            var gap = new com.architectureworkbench.intelligence.Evidence("analysis-gap-" + analysis.analysisId(),
                    "analysis-missing-evidence", "No supported findings in retained Product analysis", .1, analysis.completedAt(),
                    List.of(analysis.workspaceId(), analysis.productId(), analysis.analysisId()), List.of());
            findings = List.of(new Finding("finding-gap-" + analysis.analysisId(), Severity.INFO, "FURTHER_DISCOVERY",
                    "Review missing Product evidence", List.of(new Observation("observation-gap-" + analysis.analysisId(),
                    "product-analysis", "No source finding", List.of(gap), List.of())), .1));
        }
        var concerns = recommendation.concerns().stream().map(c -> new Concern("product-concern-" + c.toLowerCase(), c, c, "PRODUCT_ARCHITECTURE")).toList();
        return new Recommendation(recommendation.recommendationId(), recommendation.title(), recommendation.rationale(), concerns,
                findings, recommendation.impact(), recommendation.effort(), recommendation.confidenceScore(), LifecycleStatus.PROPOSED);
    }

    private ProductArchitectureAnalysisView analysis(String w, String p, String id) {
        return products.architectureAnalyses(w, p).stream().filter(a -> a.analysisId().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Analysis not found: " + id));
    }

    private void audit(String w, String p, String id, ArchitectureEventType type, Map<String,String> detail) {
        var values = new HashMap<>(detail); values.put("productId", p); values.put("recommendationId", id);
        audit.append(new ArchitectureEventEnvelope(null, type, w, ArchitectureEventSource.PRODUCT_COMPOSITION_SERVICE,
                Actor.system("product-review-board-service"), CausationId.newId("product-review"), CorrelationId.newId("product-review"),
                null, AuditRelevance.REQUIRED, MutationTarget.NEITHER, Map.copyOf(values), null));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required.");
        return value;
    }
    private static String actor(String value) { return value == null || value.isBlank() ? "api-user" : value; }
}
