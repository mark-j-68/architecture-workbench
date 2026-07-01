# Business Requirements Document — Architecture Workbench

## 1. Purpose

Architecture Workbench is an AI-assisted architecture engineering platform that helps architects and product teams transform early domain discovery artefacts into validated architecture models and generated delivery artefacts.

The platform begins with Event Storming input and progressively creates a living architecture model from which diagrams, documentation, code guidance, infrastructure, and validation reports can be generated.

## 2. Problem Statement

Software teams often move from workshops to implementation with inconsistent or incomplete architectural artefacts. Event Storming boards, diagrams, ADRs, API specifications, infrastructure, documentation, and code frequently drift apart.

AI coding tools accelerate implementation, but they often lack architectural grounding. This creates a risk of fast but inconsistent code generation.

Architecture Workbench solves this by keeping an explicit architecture model at the centre of the lifecycle.

## 3. Business Goals

1. Reduce the time from domain discovery to initial running system.
2. Maintain traceability between domain concepts, architecture decisions, generated artefacts, and implementation guidance.
3. Improve architectural quality through continuous validation and AI-assisted review.
4. Enable repeatable project bootstrapping from an Event Storming session.
5. Support local-first deployment using LocalStack and Docker.
6. Provide a foundation for Claude Code / AI-agent-assisted implementation.

## 4. Users and Personas

### 4.1 Lead Architect

Owns the architecture model, validates consistency, reviews generated artefacts, and approves architectural decisions.

### 4.2 Product Owner / Business Analyst

Contributes domain language, processes, rules, and business requirements extracted from workshops.

### 4.3 Developer

Uses generated service instructions, OpenAPI specs, event contracts, and AGENTS.md files to build implementation safely.

### 4.4 Platform Engineer

Uses generated LocalStack, Docker, and infrastructure artefacts to create local and cloud environments.

### 4.5 AI Coding Agent

Consumes generated instructions, architecture rules, service boundaries, tests, and implementation tasks.

## 5. Scope for M1

M1 is an architecture foundation milestone. It does not deliver the full product.

M1 includes:

- Product definition
- Architecture principles
- Solution architecture
- Initial C4 model
- Initial domain model
- Initial ADRs
- Roadmap

M1 excludes:

- Production implementation
- Full React UI
- Spring Boot backend
- Claude/Bedrock integration
- GitLab integration
- LocalStack deployment automation
- Full validation engine

## 6. Functional Requirements

### FR-001 — Capture Event Storming Input

The system shall support importing Event Storming artefacts, initially as image uploads.

### FR-002 — Extract Domain Model

The system shall use AI vision capabilities to extract bounded contexts, aggregates, commands, events, policies, read models, and external systems.

### FR-003 — Maintain Architecture Model

The system shall maintain a canonical architecture model as the primary source of truth.

### FR-004 — Validate Architecture Model

The system shall validate the model against deterministic architecture rules.

### FR-005 — Generate C4 Views

The system shall generate C4 architecture views from the architecture model.

### FR-006 — Generate ADRs

The system shall generate draft architecture decision records from the model and review findings.

### FR-007 — Generate Agent Instructions

The system shall generate AGENTS.md files for AI coding agents.

### FR-008 — Generate Implementation Artefacts

The system shall generate OpenAPI, AsyncAPI, BPMN, DMN, documentation, infrastructure, and service scaffolds through plugins.

### FR-009 — AI-Assisted Architecture Review

The system shall provide AI-generated architecture review findings based on the model, validation output, and generated artefacts.

### FR-010 — Local Deployment

The system shall generate and execute local deployment artefacts using Docker and LocalStack.

## 7. Non-Functional Requirements

### NFR-001 — Traceability

Every generated artefact shall be traceable back to the architecture model.

### NFR-002 — Extensibility

New generators and validators shall be pluggable.

### NFR-003 — Model Portability

The canonical model shall support import/export to YAML and JSON.

### NFR-004 — AI Provider Independence

The system shall not be coupled to one AI provider.

### NFR-005 — Local-First Development

The platform shall run locally for development and demonstration.

### NFR-006 — Human Approval

Generated artefacts and AI suggestions shall require human approval before being committed or applied.

### NFR-007 — Security

Secrets and provider tokens shall never be stored in generated architecture artefacts.

## 8. Success Criteria

M1 is successful when the project has a clear architecture foundation that can guide implementation without relying on ad hoc prompts or a single large React component.

Version 1.0 is successful when a user can move from Event Storming image to validated architecture model, generated artefacts, AI-agent build instructions, and local deployment.
