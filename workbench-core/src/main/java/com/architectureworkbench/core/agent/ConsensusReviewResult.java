package com.architectureworkbench.core.agent;

import com.architectureworkbench.core.model.audit.ActivityEnvelope;
import com.architectureworkbench.core.model.consensus.ConsensusDecision;
import java.util.List;

public record ConsensusReviewResult(
        String reviewId,
        ReviewKind kind,
        ArchitectureContext context,
        ReviewerAssessment claudeAssessment,
        ReviewerAssessment openAiCodexAssessment,
        List<String> disagreements,
        String consensusRecommendation,
        String adrDraft,
        ConsensusDecision decision,
        ActivityEnvelope auditEnvelope
) {}
