package com.architectureworkbench.core.model.consensus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConsensusDecision {
    private String decisionId;
    private String decisionType;
    private String proposalRef;
    private ConsensusOutcome outcome = ConsensusOutcome.NEEDS_HUMAN_REVIEW;
    private int roundsCompleted;
    private Instant decidedAt = Instant.now();
    private List<JudgeAssessment> assessments = new ArrayList<>();

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }
    public String getProposalRef() { return proposalRef; }
    public void setProposalRef(String proposalRef) { this.proposalRef = proposalRef; }
    public ConsensusOutcome getOutcome() { return outcome; }
    public void setOutcome(ConsensusOutcome outcome) { this.outcome = outcome; }
    public int getRoundsCompleted() { return roundsCompleted; }
    public void setRoundsCompleted(int roundsCompleted) { this.roundsCompleted = roundsCompleted; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public List<JudgeAssessment> getAssessments() { return assessments; }
    public void setAssessments(List<JudgeAssessment> assessments) { this.assessments = Objects.requireNonNullElseGet(assessments, ArrayList::new); }
}
