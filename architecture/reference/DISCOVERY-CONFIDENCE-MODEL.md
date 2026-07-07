# Discovery Confidence Model

Confidence explains how strongly discovery output is supported by evidence. It
must expose uncertainty rather than hide it.

## Confidence Categories

### Observed Fact

An observed fact is directly present in a source.

Example:

```text
pom.xml declares dependency com.example:pricing-client:1.4.2.
```

Default confidence: high, assuming source was readable and parser succeeded.

Confidence weakens when the source is generated, stale, unreadable in part, or
conflicting with stronger evidence.

### Deterministic Inference

A deterministic inference follows a stable rule from observed facts.

Example:

```text
Class annotated @RestController exposes an HTTP API component.
```

Default confidence: high to medium, depending on rule specificity.

Confidence strengthens when multiple deterministic signals agree, such as
`@RestController`, route mappings, and OpenAPI operations.

### Heuristic Inference

A heuristic inference is a candidate conclusion from patterns, naming,
structure, or partial evidence.

Example:

```text
Package naming and dependency cohesion suggest a bounded context candidate.
```

Default confidence: medium to low.

Heuristic output must use candidate language. It must not be presented as a
fact.

### AI-Assisted Inference

An AI-assisted inference uses semantic model interpretation.

Example:

```text
Semantic analysis suggests these classes belong to the Underwriting domain.
```

Default confidence: low to medium unless corroborated by deterministic evidence.

AI-assisted inference must be optional, labelled, traceable to prompt/model/tool
metadata where used, and separated from deterministic discovery.

## Confidence Rules

Confidence is calculated from:

- evidence type
- evidence quality
- parser specificity
- source recency
- corroboration
- conflicts
- plugin reliability
- inference mode
- review validation where available

Suggested normalized factors:

```text
confidence =
  source_quality * 0.20 +
  parser_specificity * 0.20 +
  evidence_directness * 0.20 +
  corroboration * 0.20 +
  recency * 0.10 +
  conflict_penalty_adjustment * 0.10
```

AI-assisted inference should include an additional model uncertainty penalty
unless supported by deterministic evidence.

## Corroboration

Multiple independent evidence sources can strengthen confidence.

Examples:

- a controller class and OpenAPI operation describe the same endpoint
- a message producer and infrastructure descriptor reference the same topic
- a Maven dependency and source import point to the same library
- CODEOWNERS and service catalogue identify the same team

Corroboration must be independent. Repeating the same source through generated
files should not artificially inflate confidence.

## Conflict Handling

Conflicting evidence should lower confidence and produce diagnostics.

Examples:

- OpenAPI spec declares an endpoint not found in controller code
- pipeline deploys a service not found in build outputs
- ownership docs conflict with CODEOWNERS
- dependency lock file differs from declared build version

Conflicts should not be hidden by averaging. Important conflicts should become
inputs to AIM findings.

## Confidence Language

Use precise language:

- observed: directly found in source
- inferred: produced by deterministic rule
- candidate: heuristic or uncertain inference
- suggested: AI-assisted or low-confidence inference

Do not use confidence to disguise uncertainty. A low-confidence but important
signal should remain visible with clear caveats.

## AIM Interaction

Evidence and observations carry confidence into AIM. AIM findings and
recommendations may calculate their own confidence from:

- evidence confidence
- observation confidence
- number of supporting sources
- severity of conflicts
- Review Board validation

Discovery confidence is an input to reasoning, not the reasoning itself.
