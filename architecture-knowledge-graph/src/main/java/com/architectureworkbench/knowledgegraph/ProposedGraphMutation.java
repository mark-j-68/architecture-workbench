package com.architectureworkbench.knowledgegraph;

public sealed interface ProposedGraphMutation permits ProposedElementAddition, ProposedRelationshipAddition {
    ProposedChangeType changeType();
}
