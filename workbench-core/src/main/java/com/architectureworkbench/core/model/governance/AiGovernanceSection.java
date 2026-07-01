package com.architectureworkbench.core.model.governance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AiGovernanceSection {
    private boolean consensusRequired = true;
    private int maxConsensusRounds = 3;
    private String fallback = "HUMAN_REVIEW";
    private List<AiJudge> judges = new ArrayList<>();
    private List<String> decisionTypesRequiringConsensus = new ArrayList<>();

    public boolean isConsensusRequired() { return consensusRequired; }
    public void setConsensusRequired(boolean consensusRequired) { this.consensusRequired = consensusRequired; }
    public int getMaxConsensusRounds() { return maxConsensusRounds; }
    public void setMaxConsensusRounds(int maxConsensusRounds) { this.maxConsensusRounds = maxConsensusRounds; }
    public String getFallback() { return fallback; }
    public void setFallback(String fallback) { this.fallback = fallback; }
    public List<AiJudge> getJudges() { return judges; }
    public void setJudges(List<AiJudge> judges) { this.judges = Objects.requireNonNullElseGet(judges, ArrayList::new); }
    public List<String> getDecisionTypesRequiringConsensus() { return decisionTypesRequiringConsensus; }
    public void setDecisionTypesRequiringConsensus(List<String> decisionTypesRequiringConsensus) { this.decisionTypesRequiringConsensus = Objects.requireNonNullElseGet(decisionTypesRequiringConsensus, ArrayList::new); }
}
