# M2 Implementation Notes

## Scope

M2 delivers the canonical Architecture Model and the first Java implementation of `workbench-core`.

This is not yet the Spring Boot backend or React UI. Those belong to later milestones. M2 creates the model they will depend on.

## Delivered

- Java 21 Maven module: `workbench-core`.
- Canonical `ArchitectureModel` object hierarchy.
- YAML schema for the architecture model.
- Example mortgage origination architecture YAML.
- Validation framework primitives.
- Three starter validation rules.
- Graph model primitives.
- Unit test proving validation framework behaviour.

## How to run

```bash
cd workbench-core
mvn test

## Next Milestone

M3 should wrap `workbench-core` in a Spring Boot API that can:

- create a workspace
- load/save architecture YAML
- validate the model
- expose validation findings
- expose graph projection endpoints
- prepare for AI-powered import/refinement

## Governance Update

The following files were added after the initial M2 pack:

- `model/governance/*` — configuration model for AI consensus, immutable audit and PII protection.
- `model/audit/*` — immutable non-PII activity envelope and protected payload reference.
- `model/consensus/*` — consensus decision, judge assessment and outcome model.
- `validation/AiConsensusConfigurationRule.java`.
- `validation/RegulatoryAuditConfigurationRule.java`.

Implementation should ensure that prompt text, uploaded images, extracted artefacts and model responses are classified before being logged or embedded. The immutable envelope must be safe to retain even after sensitive payloads are shredded.
