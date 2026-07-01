package com.architectureworkbench.core.agent;

import com.architectureworkbench.core.model.ArchitectureModel;
import java.util.ArrayList;
import java.util.List;

public class DeterministicReviewerClient implements ReviewerClient {
    private final String reviewerId;
    private final String provider;
    private final String model;

    public DeterministicReviewerClient(String reviewerId, String provider, String model) {
        this.reviewerId = reviewerId;
        this.provider = provider;
        this.model = model;
    }

    public static DeterministicReviewerClient claude() {
        return new DeterministicReviewerClient("claude-architecture-reviewer", "CLAUDE", "claude-reviewer-adapter");
    }

    public static DeterministicReviewerClient openAiCodex() {
        return new DeterministicReviewerClient("openai-codex-architecture-reviewer", "OPENAI_CODEX", "codex-reviewer-adapter");
    }

    @Override public String reviewerId() { return reviewerId; }
    @Override public String provider() { return provider; }
    @Override public String model() { return model; }

    @Override
    public ReviewerAssessment assess(ReviewRequest request, ArchitectureContext context, String prompt) {
        ArchitectureModel architectureModel = request.model();
        List<String> strengths = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (context.immutableAuditRequired() && context.piiEncrypted() && context.cryptoShreddingRequired()) {
            strengths.add("Regulatory controls preserve immutable envelopes, encrypted sensitive payloads, and crypto-shredding.");
        }
        if (context.boundedContextCount() > 0) {
            strengths.add("Domain model exposes bounded contexts for review traceability.");
        }
        if (context.aggregateCount() == 0) {
            risks.add("No aggregates are defined, so DDD validation cannot confirm transactional boundaries.");
            recommendations.add("Define aggregate roots and invariants before accepting implementation scaffolds.");
        }
        if (context.commandCount() > 0 && context.eventCount() == 0) {
            risks.add("Commands exist without corresponding domain events, reducing observability of business state changes.");
            recommendations.add("Add emitted domain events for material aggregate state transitions.");
        }
        if (architectureModel.getGovernance().getAi().isConsensusRequired()
                && architectureModel.getGovernance().getAi().getJudges().stream().filter(j -> j.isEnabled()).count() < 2) {
            risks.add("Consensus is required but fewer than two enabled AI judges are configured.");
            recommendations.add("Configure Claude and OpenAI/Codex judges before relying on automated acceptance.");
        }

        if ("CLAUDE".equals(provider) && request.kind() == ReviewKind.DDD_VALIDATION) {
            recommendations.add("Review aggregate invariants against ubiquitous language before generating service interfaces.");
        }
        if ("OPENAI_CODEX".equals(provider)) {
            recommendations.add("Keep all model changes behind application services and validation rules.");
        }

        String verdict = risks.stream().anyMatch(risk -> risk.toLowerCase().contains("fewer than two") || risk.toLowerCase().contains("no aggregates"))
                ? "NEEDS_REVISION"
                : "ACCEPTABLE_WITH_CONTROLS";
        double confidence = risks.isEmpty() ? 0.82 : 0.67;
        String response = "Verdict: %s. Risks: %d. Recommendations: %d.".formatted(verdict, risks.size(), recommendations.size());

        return new ReviewerAssessment(
                reviewerId,
                provider,
                model,
                verdict,
                confidence,
                strengths,
                risks,
                recommendations,
                prompt,
                response,
                null
        );
    }
}
