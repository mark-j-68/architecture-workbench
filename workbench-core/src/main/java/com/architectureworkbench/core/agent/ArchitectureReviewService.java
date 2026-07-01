package com.architectureworkbench.core.agent;

import com.architectureworkbench.core.audit.ImmutableActivityLog;
import com.architectureworkbench.core.model.audit.ActivityEnvelope;
import com.architectureworkbench.core.model.consensus.ConsensusDecision;
import com.architectureworkbench.core.model.consensus.ConsensusOutcome;
import com.architectureworkbench.core.model.consensus.JudgeAssessment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ArchitectureReviewService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReviewerClient claudeReviewer;
    private final ReviewerClient openAiCodexReviewer;
    private final ArchitectureContextBuilder contextBuilder;
    private final ImmutableActivityLog auditLog;
    private final List<ConsensusReviewResult> history = new ArrayList<>();

    public ArchitectureReviewService(ImmutableActivityLog auditLog) {
        this(
                DeterministicReviewerClient.claude(),
                DeterministicReviewerClient.openAiCodex(),
                new ArchitectureContextBuilder(),
                auditLog
        );
    }

    public ArchitectureReviewService(
            ReviewerClient claudeReviewer,
            ReviewerClient openAiCodexReviewer,
            ArchitectureContextBuilder contextBuilder,
            ImmutableActivityLog auditLog
    ) {
        this.claudeReviewer = claudeReviewer;
        this.openAiCodexReviewer = openAiCodexReviewer;
        this.contextBuilder = contextBuilder;
        this.auditLog = auditLog;
    }

    public ConsensusReviewResult runArchitectureReview(ReviewRequest request) {
        return runReview(new ReviewRequest(request.workspaceId(), request.actorRef(), ReviewKind.ARCHITECTURE, request.model(), request.question()));
    }

    public ConsensusReviewResult runDddValidationReview(ReviewRequest request) {
        return runReview(new ReviewRequest(request.workspaceId(), request.actorRef(), ReviewKind.DDD_VALIDATION, request.model(), request.question()));
    }

    public ConsensusReviewResult runConsensusReview(ReviewRequest request) {
        return runReview(new ReviewRequest(request.workspaceId(), request.actorRef(), ReviewKind.CONSENSUS, request.model(), request.question()));
    }

    public synchronized List<ConsensusReviewResult> reviewHistory(String workspaceId) {
        return history.stream()
                .filter(result -> Objects.equals(result.context().workspaceId(), workspaceId))
                .toList();
    }

    private synchronized ConsensusReviewResult runReview(ReviewRequest request) {
        String reviewId = "review-" + UUID.randomUUID();
        String correlationId = reviewId;
        ArchitectureContext context = contextBuilder.build(request.workspaceId(), request.model());
        String prompt = buildPrompt(request, context);
        ReviewerAssessment claude = claudeReviewer.assess(request, context, prompt);
        ReviewerAssessment openAiCodex = openAiCodexReviewer.assess(request, context, prompt);
        List<String> disagreements = compare(claude, openAiCodex);
        String recommendation = recommend(claude, openAiCodex, disagreements);
        String adrDraft = adrDraft(request, recommendation, disagreements);
        ConsensusDecision decision = decision(reviewId, request, claude, openAiCodex, disagreements);

        String protectedTrace = protectedTrace(reviewId, request, context, claude, openAiCodex, disagreements, recommendation, adrDraft, decision);
        ActivityEnvelope envelope = auditLog.append(
                request.workspaceId(),
                request.actorRef(),
                "AI_%s_REVIEW_COMPLETED".formatted(request.kind()),
                correlationId,
                protectedTrace
        );

        claude = withActivityId(claude, envelope.getActivityId());
        openAiCodex = withActivityId(openAiCodex, envelope.getActivityId());
        decision.setAssessments(List.of(toJudgeAssessment(claude), toJudgeAssessment(openAiCodex)));

        ConsensusReviewResult result = new ConsensusReviewResult(
                reviewId,
                request.kind(),
                context,
                claude,
                openAiCodex,
                disagreements,
                recommendation,
                adrDraft,
                decision,
                envelope
        );
        history.add(result);
        return result;
    }

    private static String buildPrompt(ReviewRequest request, ArchitectureContext context) {
        return """
                Review kind: %s
                Workspace: %s
                Question: %s
                Context: boundedContexts=%d aggregates=%d commands=%d events=%d services=%d integrations=%d
                Regulatory controls: immutableAudit=%s piiEncrypted=%s cryptoShredding=%s
                Return verdict, rationale, risks, recommendations, and any ADR-worthy decision.
                """.formatted(
                request.kind(),
                request.workspaceId(),
                request.question(),
                context.boundedContextCount(),
                context.aggregateCount(),
                context.commandCount(),
                context.eventCount(),
                context.serviceCount(),
                context.integrationCount(),
                context.immutableAuditRequired(),
                context.piiEncrypted(),
                context.cryptoShreddingRequired()
        );
    }

    private static List<String> compare(ReviewerAssessment first, ReviewerAssessment second) {
        LinkedHashSet<String> disagreements = new LinkedHashSet<>();
        if (!Objects.equals(first.verdict(), second.verdict())) {
            disagreements.add("Verdict differs: %s returned %s, %s returned %s.".formatted(
                    first.provider(), first.verdict(), second.provider(), second.verdict()));
        }
        first.recommendations().stream()
                .filter(recommendation -> !second.recommendations().contains(recommendation))
                .forEach(recommendation -> disagreements.add("%s unique recommendation: %s".formatted(first.provider(), recommendation)));
        second.recommendations().stream()
                .filter(recommendation -> !first.recommendations().contains(recommendation))
                .forEach(recommendation -> disagreements.add("%s unique recommendation: %s".formatted(second.provider(), recommendation)));
        return List.copyOf(disagreements);
    }

    private static String recommend(ReviewerAssessment claude, ReviewerAssessment openAiCodex, List<String> disagreements) {
        if (claude.verdict().equals(openAiCodex.verdict()) && disagreements.isEmpty()) {
            return "Consensus reached: %s. Proceed with recorded controls and traceability.".formatted(claude.verdict());
        }
        if (claude.verdict().equals(openAiCodex.verdict())) {
            return "Consensus verdict is %s, with reviewer-specific recommendations requiring owner review.".formatted(claude.verdict());
        }
        return "No full consensus. Escalate to human architecture owner before changing the architecture model.";
    }

    private static String adrDraft(ReviewRequest request, String recommendation, List<String> disagreements) {
        return """
                # ADR Draft: AI-Assisted %s Review

                ## Status
                Proposed

                ## Context
                The architecture model was reviewed by Claude and OpenAI/Codex under regulated-workspace controls.

                ## Decision
                %s

                ## Consequences
                - Prompt, response, reviewer, tool, and outcome traces are stored via encrypted protected payload references.
                - The immutable audit envelope stores only hashes and references.
                - Disagreements: %s
                """.formatted(request.kind(), recommendation, disagreements.isEmpty() ? "None" : String.join("; ", disagreements));
    }

    private static ConsensusDecision decision(String reviewId, ReviewRequest request, ReviewerAssessment claude, ReviewerAssessment openAiCodex, List<String> disagreements) {
        ConsensusDecision decision = new ConsensusDecision();
        decision.setDecisionId("decision-" + reviewId);
        decision.setDecisionType(request.kind().name());
        decision.setProposalRef(reviewId);
        decision.setRoundsCompleted(1);
        decision.setDecidedAt(Instant.now());
        if (!claude.verdict().equals(openAiCodex.verdict())) {
            decision.setOutcome(ConsensusOutcome.NEEDS_HUMAN_REVIEW);
        } else if (!disagreements.isEmpty()) {
            decision.setOutcome(ConsensusOutcome.NEEDS_REVISION);
        } else if (claude.verdict().contains("ACCEPTABLE")) {
            decision.setOutcome(ConsensusOutcome.ACCEPTED);
        } else {
            decision.setOutcome(ConsensusOutcome.NEEDS_REVISION);
        }
        return decision;
    }

    private static ReviewerAssessment withActivityId(ReviewerAssessment assessment, String activityId) {
        return new ReviewerAssessment(
                assessment.reviewerId(),
                assessment.provider(),
                assessment.model(),
                assessment.verdict(),
                assessment.confidence(),
                assessment.strengths(),
                assessment.risks(),
                assessment.recommendations(),
                assessment.prompt(),
                assessment.response(),
                activityId
        );
    }

    private static JudgeAssessment toJudgeAssessment(ReviewerAssessment assessment) {
        JudgeAssessment judge = new JudgeAssessment();
        judge.setJudgeId(assessment.reviewerId());
        judge.setProvider(assessment.provider());
        judge.setModel(assessment.model());
        judge.setVerdict(assessment.verdict());
        judge.setConfidence(assessment.confidence());
        judge.setRationaleRef("protected-payload:" + assessment.activityId());
        judge.setActivityId(assessment.activityId());
        return judge;
    }

    private static String protectedTrace(
            String reviewId,
            ReviewRequest request,
            ArchitectureContext context,
            ReviewerAssessment claude,
            ReviewerAssessment openAiCodex,
            List<String> disagreements,
            String recommendation,
            String adrDraft,
            ConsensusDecision decision
    ) {
        try {
            return MAPPER.writeValueAsString(new ReviewTrace(
                    reviewId,
                    request.kind(),
                    context,
                    List.of("architecture_context", "claude_review", "openai_codex_review", "consensus_compare", "adr_draft"),
                    claude,
                    openAiCodex,
                    disagreements,
                    recommendation,
                    adrDraft,
                    decision.getOutcome().name()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize protected review trace", e);
        }
    }

    private record ReviewTrace(
            String reviewId,
            ReviewKind kind,
            ArchitectureContext context,
            List<String> toolsUsed,
            ReviewerAssessment claudeAssessment,
            ReviewerAssessment openAiCodexAssessment,
            List<String> disagreements,
            String consensusRecommendation,
            String adrDraft,
            String decisionOutcome
    ) {}
}
