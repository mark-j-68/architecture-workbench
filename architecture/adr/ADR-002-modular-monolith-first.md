# ADR-002 — Build as a Modular Monolith First

## Status

Accepted

## Context

Architecture Workbench will eventually contain model management, validation, generation, AI review, graph analysis, Git integration, and deployment orchestration. These could be separate services, but early distribution would add complexity before the domain stabilises.

## Decision

The initial backend shall be implemented as a modular monolith using Java 21 and Spring Boot.

Modules should be separated by package/module boundaries:

- workbench-core
- workbench-validation
- workbench-generators
- workbench-ai
- workbench-api

Physical service separation may be revisited later.

## Consequences

Positive:

- Faster initial development
- Easier local deployment
- Lower operational complexity
- Clear path to extract services later if needed

Negative:

- Requires discipline to maintain module boundaries
- May need refactoring if plugin execution becomes resource-heavy
