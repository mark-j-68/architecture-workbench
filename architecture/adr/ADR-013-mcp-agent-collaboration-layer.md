# ADR-013 — MCP Agent Collaboration Layer

## Status

Accepted

## Context

Architecture Workbench will collaborate with AI agents such as Claude and OpenAI/Codex reviewers. These agents need architecture context, validation results, projection data, review history, and evidence. They may propose findings, ADR drafts, and remediation options.

However, regulated architecture governance requires that agents do not directly mutate the canonical architecture knowledge graph. Direct mutation would bypass validation, audit, approval, PII controls, and cryptographic shredding boundaries.

## Decision

Introduce an MCP Agent Collaboration Layer as a controlled boundary over the architecture knowledge graph.

The MCP layer may expose controlled tools for:

- reading graph context
- retrieving projection views
- running validation and healthchecks
- retrieving review history
- recording proposed review findings
- preparing ADR drafts
- tracing prompts, responses, tools used, model identifiers, evidence references, and decision outcomes

The MCP layer must not expose raw graph mutation tools. Accepted changes are applied only through validated application services and human approval workflows.

The AI Architecture Review Board is the durable review surface. It stores Claude and OpenAI/Codex assessments, disagreements, consensus recommendations, generated ADR drafts, linked evidence, linked risks, and final decision outcomes.

## Consequences

Positive:

- Agents receive structured, governed context.
- Agent activity is auditable and traceable.
- Direct model mutation is prevented.
- Provider implementations can vary without changing the graph core.
- Human review remains the gate for material architecture changes.

Negative:

- Agent workflows require more orchestration than direct prompt-to-model updates.
- Tool schemas must be carefully versioned.
- Review Board records may grow large and require protected payload storage.

## Scope

M3 defines the MCP boundary, tool categories, traceability obligations, and Review Board model. It does not implement live Claude, OpenAI, or Codex calls.
