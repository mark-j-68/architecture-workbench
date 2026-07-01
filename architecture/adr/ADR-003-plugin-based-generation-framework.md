# ADR-003 — Use Plugin-Based Generation Framework

## Status

Accepted

## Context

The platform will need to generate many artefact types: C4, ADRs, OpenAPI, AsyncAPI, BPMN, DMN, CloudFormation, Docker Compose, AGENTS.md, documentation, and test assets.

Hardcoding generators into the UI or API would make the system difficult to extend.

## Decision

Generation shall be implemented through a plugin interface.

Each generator plugin shall declare:

- id
- display name
- supported input model features
- output artefact types
- generation method

## Consequences

Positive:

- New generators can be added incrementally
- Clear separation of concerns
- Easier testing
- Enables future third-party extension

Negative:

- Requires plugin lifecycle design
- Requires consistent generation result contracts
