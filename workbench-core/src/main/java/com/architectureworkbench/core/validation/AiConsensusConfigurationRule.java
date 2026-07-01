package com.architectureworkbench.core.validation;

import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.governance.AiGovernanceSection;
import com.architectureworkbench.core.model.validation.Severity;
import com.architectureworkbench.core.model.validation.ValidationFinding;
import java.util.ArrayList;
import java.util.List;

public class AiConsensusConfigurationRule implements ValidationRule {
    @Override public String id() { return "GOV-AI-001"; }
    @Override public String description() { return "AI consensus requires at least two enabled judges and a bounded number of rounds."; }

    @Override
    public List<ValidationFinding> validate(ArchitectureModel model) {
        List<ValidationFinding> findings = new ArrayList<>();
        AiGovernanceSection ai = model.getGovernance().getAi();
        long enabledJudges = ai.getJudges().stream().filter(j -> j.isEnabled()).count();
        if (ai.isConsensusRequired() && enabledJudges < 2) {
            findings.add(new ValidationFinding(id(), Severity.ERROR,
                    "LLM consensus is required but fewer than two enabled judges are configured.",
                    "governance.ai.judges"));
        }
        if (ai.getMaxConsensusRounds() < 1 || ai.getMaxConsensusRounds() > 5) {
            findings.add(new ValidationFinding(id(), Severity.WARNING,
                    "Consensus rounds should normally be between 1 and 5 to avoid unbounded AI loops.",
                    "governance.ai.maxConsensusRounds"));
        }
        return findings;
    }
}
