# ADR-024 - Review Board as Governed Decision Workflow

## Status

Accepted

## Context

Architecture Workbench now separates architecture intelligence, recommendation
candidates, proposed graph changes, and accepted graph mutations. The next
governance requirement is to review recommendation candidates and proposed
changes before anyone explicitly accepts, rejects, or defers them.

Without a Review Board workflow, proposed changes could be accepted by a single
caller without a durable record of participants, votes, disagreement,
conditions, or requests for further evidence.

## Decision

Introduce the Review Board as a governed decision workflow.

The Review Board workflow opens a session over one or more AIM recommendation
candidates and proposed architecture changes. Participants can include human
architects, DDD reviewers, security reviewers, cloud reviewers, compliance
reviewers, delivery reviewers, and automated reviewer stubs.

Participants vote to:

- approve
- reject
- defer
- request more evidence
- approve with conditions

The workflow derives a `ReviewBoardDecision` that can recommend:

- accepting a proposed change
- rejecting a proposed change
- deferring a proposed change
- requesting further discovery
- requesting further review

The Review Board does not automatically apply proposed changes. Actual graph
mutation remains explicit through `ProposedChangeService`, which routes accepted
mutations through validated graph services.

## Consequences

Recommendation candidates and proposed graph changes now have a governed review
step before canonical graph mutation.

Review sessions emit typed `ReviewRequested` and `ReviewCompleted` events using
the architecture event envelope, preserving workspace and correlation metadata.

Conflicting votes, conditional approvals, and requests for evidence become
first-class decision outcomes rather than informal comments.

This decision does not introduce AI providers, persistence, REST APIs, UI,
event sourcing, or automatic graph mutation from Review Board decisions.

## Boundaries

Review Board decisions are recommendations for governance action. They are not
graph mutations.

Future APIs, UI flows, MCP tools, and providers must use the Review Board
workflow for governed recommendations and proposed changes, then call explicit
proposal acceptance only when governance approves.
