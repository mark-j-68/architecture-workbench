# ADR-004 — Abstract AI Providers

## Status

Accepted

## Context

The prototype called Claude directly from React. For a serious workbench, AI calls should not be made from the frontend and should not be coupled to a single provider.

The preferred initial provider is Claude via AWS Bedrock, but future providers may include OpenAI, Anthropic direct API, Gemini, or local models.

## Decision

The backend shall expose an AI provider abstraction.

The frontend shall never call AI providers directly.

M3 adds an MCP Agent Collaboration Layer and an AI Architecture Review Board as the governed boundary around provider usage. Provider adapters may produce assessments and proposals, but they must not mutate the architecture knowledge graph directly.

Initial capabilities:

- text completion
- vision extraction
- model refinement
- architecture review
- fix suggestion generation

## M3 Scope Note

M3 defines the provider boundary, collaboration model, prompt/response traceability, and Review Board records. It does not implement live Claude, OpenAI, or Codex calls.

## Consequences

Positive:

- Provider independence
- Better security
- Easier testing and mocking
- Centralised prompt management

Negative:

- Slightly more backend complexity
- Provider-specific features require careful abstraction
