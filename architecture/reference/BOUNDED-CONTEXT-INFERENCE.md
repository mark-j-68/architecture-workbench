# Deterministic Bounded-Context Candidate Inference

## Purpose

This document defines Release 0.4 inference of bounded-context candidates from retained Product evidence. It does not claim to discover definitive domain boundaries. A bounded context is a semantic boundary with its own language, model, rules, ownership, and contracts; repositories and Product Modules are useful evidence but are not bounded contexts by default.

This model follows [ADR-048](../adr/ADR-048-keep-semantic-inference-candidates-separate-from-accepted-facts.md) and the observed/intended boundary in [ADR-047](../adr/ADR-047-separate-observed-and-intended-architecture.md).

## Terminology

| Term | Meaning |
|---|---|
| Evidence | Immutable observed or supplied material with provenance, time, confidence, and references. |
| Observation | A deterministic statement derived from evidence, such as “schema X is accessed by repositories A and B.” |
| Semantic assertion | A typed subject-predicate-object statement used in observed or intended projections. |
| Candidate region | A stable set of implementation units evaluated as one possible semantic boundary. |
| Bounded-context candidate | An inferred candidate region with a name suggestion, rule derivation, support, counter-evidence, confidence, and limitations. |
| Unresolved region | Units for which evidence is insufficient or contradictory; it is not a low-confidence candidate. |
| Approved candidate | A reviewed option eligible to produce a concrete graph proposal. It is still not canonical fact. |
| Accepted semantic fact | A graph element or relationship created only after explicit acceptance of a governed `ProposedArchitectureChange`. |

`CONFIRMED` confidence and `ACCEPTED` lifecycle are not synonyms. Confidence describes evidence support; acceptance describes authority.

## Evidence signals

### Structural signals

- repository, Product Module, build-module, source-set, and package boundaries;
- package dependency direction, cycles, fan-in/fan-out, and strongly connected components;
- deployable and configuration boundaries where deterministic evidence exists;
- shared library and shared domain model dependencies.

Structural signals can shape candidate regions but cannot establish domain semantics alone.

### Ownership signals

- explicit Product repository and contract ownership metadata;
- CODEOWNERS or equivalent discovered ownership evidence;
- schema, API, event, and deployment ownership;
- conflicting, shared, or missing ownership.

Explicit metadata is evidence supplied by an owner, not automatically accepted architectural intent.

### Data signals

- schema, database, table, or persistence namespace access;
- one clear writer with local readers;
- multiple writers or cross-region direct data access;
- data contracts and schema publication where available.

Schema names alone do not establish domain meaning. Direct access is strong counter-evidence to an asserted ownership boundary.

### Contract and interaction signals

- API providers and consumers;
- published and consumed event contracts;
- command providers and handlers;
- synchronous calls, messaging channels, and integration direction;
- contract version and compatibility evidence.

Contract identity must use the conservative Release 0.3 identity rules. Ambiguous identities remain conflicts.

### Vocabulary signals

- repeated domain terms in type, package, API operation, event, command, and schema identifiers;
- term concentration inside a candidate region;
- the same term used with incompatible structures or meanings across regions.

Vocabulary is tokenized and normalized by a versioned deterministic algorithm. Stop words, framework terms, and generic technical words are excluded. Vocabulary never acts as the sole qualifying signal.

### Change-coupling signals

Change coupling is considered only when retained revision evidence explicitly records comparable co-change sets. Release 0.4 does not fetch remote history. File timestamps, matching versions, or proximity in the current tree are not substitutes for commit evidence.

### Explicit declarations

- Product Modules and repository assignments;
- accepted bounded-context graph elements and relationships;
- accepted architecture constraints and ADR-linked intent.

Product Modules seed candidate evaluation. Accepted graph state supplies intended comparison. Neither causes observed code to be classified as conforming without evidence.

## Candidate model

A `BoundedContextCandidate` should contain:

- candidate ID and deterministic key;
- workspace ID, Product ID, composition version, and inference-run ID;
- rule-set ID and version;
- proposed label plus label derivation;
- member units, each with kind and stable source identity;
- supporting observations and evidence IDs grouped by signal family;
- counter-observations and counter-evidence IDs;
- related explicit Product Module and accepted graph IDs;
- boundary confidence band and explanation;
- evidence sufficiency status and missing signal families;
- limitations;
- lifecycle state, superseded candidate ID, review references, and timestamps.

The candidate stores references to existing evidence. It does not copy source evidence or become an `ArchitectureElement`.

## Confidence semantics

Release 0.4 uses deterministic qualification bands rather than an opaque weighted average:

| Band | Qualification |
|---|---|
| `HIGH` | At least three independent signal families support the same boundary; at least one is semantic, data, contract, or explicit ownership; no unresolved strong counter-signal. |
| `MEDIUM` | At least two independent families support the boundary; at least one is semantic, data, contract, or ownership; counter-evidence is bounded and explained. |
| `LOW` | A candidate passes the minimum rule using one qualifying semantic/ownership family plus structural support, but material evidence is missing. It is shown for review and cannot be presented as likely fact. |
| `INSUFFICIENT_EVIDENCE` | The minimum rule is not met. Emit an unresolved region or diagnostic instead of a candidate. |

Independent signal families prevent ten package-name matches from masquerading as ten independent reasons. Source evidence confidence limits the confidence of derived observations. A candidate cannot have higher confidence than its weakest required qualifying observation.

## Counter-evidence

Counter-evidence is retained beside support and may reduce the band or produce an unresolved region. Examples include:

- dense bidirectional dependencies across the proposed boundary;
- multiple writers to supposedly context-owned data;
- shared mutable domain types;
- one owner and release stream spanning both sides where independence was claimed;
- vocabulary distributed evenly across candidate regions;
- APIs bypassed by direct implementation or database access;
- explicit accepted intent that defines a different boundary;
- incomplete discovery for a repository central to the candidate.

Conflicting signals are never averaged away. The result includes each conflict and the deterministic rule used to resolve or defer it.

## Deterministic inference process

### 1. Establish comparable implementation units

Create stable units from explicit Product Modules, repositories, build modules, packages, deployables, schemas, and contracts. Record source identity and discovery-run provenance. Units without stable identity are diagnostics.

### 2. Build a typed evidence graph

Create a read-only inference graph whose edges retain interaction type, direction, confidence, and evidence IDs. This is an analysis projection, not the Architecture Knowledge Graph.

### 3. Apply hard separation and union rules

Versioned rules create candidate seeds:

- an accepted explicit Product Module may seed a region but does not force it;
- a cohesive build/package cluster with one explicit owner may seed a region;
- a schema with one writer plus locally concentrated domain vocabulary may seed a region;
- an API or event publisher with cohesive handlers/model and ownership may seed a region;
- units joined only by generic libraries, framework code, or generated clients are not merged;
- multiple mutable data writers or a strongly connected implementation cluster block unsupported separation.

Rules run in a stable order over sorted identifiers. Any clustering algorithm must be deterministic for identical input and expose its inputs and rule version.

### 4. Qualify semantic support

A seed becomes a candidate only when structural support is joined by an independent data, contract, vocabulary, explicit ownership, or explicit semantic declaration signal. Structure-only seeds become unresolved regions.

### 5. Evaluate boundary crossings

Classify each cross-region edge as explicit contract interaction, allowed shared infrastructure, direct implementation dependency, shared data, ambiguous identity, or unknown. Use it as support or counter-evidence according to a published rule.

### 6. Produce candidates and unresolved regions

Produce immutable results for the exact composition and evidence set. Stable deterministic keys permit recurrence tracking, but each inference run retains its own candidate snapshot.

## Initial rule catalogue

| Rule | Required support | Result |
|---|---|---|
| `EXPLICIT_MODULE_WITH_SEMANTIC_SUPPORT` | Product Module plus ownership, vocabulary, data, or contract cohesion | candidate matching or refining the module |
| `OWNED_DATA_AND_MODEL_COHESION` | single writer/owner plus cohesive model/vocabulary | candidate around the owner and model |
| `CONTRACT_PROVIDER_COHESION` | API/event provider plus cohesive handlers/model and ownership | provider-side candidate |
| `EVENT_LANGUAGE_COHESION` | related published events and handlers with concentrated vocabulary | candidate when backed by another family |
| `STRUCTURAL_CLUSTER_ONLY` | package/module cohesion only | unresolved region, not candidate |
| `SHARED_MUTABLE_MODEL_CONFLICT` | shared mutable model or multiple writers | counter-evidence; merge, refine, or unresolved |
| `CROSS_BOUNDARY_CYCLE_CONFLICT` | dense bidirectional/cyclic implementation dependencies | counter-evidence and boundary-quality finding |
| `INTENT_OBSERVATION_MISMATCH` | accepted context differs from observed candidate | retain both; produce comparison input, never overwrite intent |

## Review and acceptance lifecycle

```text
CANDIDATE
  → UNDER_REVIEW
  → APPROVED_FOR_PROPOSAL | REJECTED | REFINEMENT_REQUIRED | DEFERRED
REFINEMENT_REQUIRED
  → SUPERSEDED by a new candidate revision
APPROVED_FOR_PROPOSAL
  → one or more ProposedArchitectureChanges
accepted proposal
  → accepted graph element/relationships
```

- Review records votes, rationale, disagreement, evidence requests, and candidate revision.
- Refinement creates a new immutable candidate with an explicit supersedes link; it never edits the earlier record.
- Approval means the candidate is suitable to formulate as intended architecture. It does not create graph state.
- Concrete element and relationship proposals retain candidate, evidence, finding, recommendation, and review references.
- Only explicit proposal acceptance creates an accepted semantic fact.

The existing Review Board can host the review through a small adapter, following ADR-046. Candidate lifecycle vocabulary should remain distinct from recommendation lifecycle vocabulary at the API boundary.

## False positives and false negatives

### False-positive controls

- no names-only inference;
- independent-family requirement;
- generic/framework vocabulary exclusion;
- explicit counter-evidence and missing-evidence display;
- deterministic endpoint and identity validation;
- human refinement before proposal;
- no automatic graph mutation.

### False-negative controls

- preserve low-confidence candidates that meet the minimum semantic rule;
- show unresolved regions and the evidence needed to resolve them;
- allow an architect to propose a refined candidate while recording supplied evidence and rationale;
- retain rejected/superseded candidates so later evidence can explain recurrence;
- do not treat “not inferred” as “does not exist.”

## Relationship to DDD and Product Modules

A bounded context is a DDD semantic boundary. Product Module is the existing user-defined Product partition and may align with a repository, packaging unit, deployable, capability, or bounded context. Release 0.4 measures alignment but does not redefine Product Module.

One Product Module may contain several bounded-context candidates. One candidate may span modules or repositories. Such mismatches are findings or refinement inputs, not automatic defects.

## Examples

### Strong candidate

`pricing-service` owns a pricing schema, publishes `QuoteCalculated`, exposes a versioned pricing API, contains concentrated pricing vocabulary, and has one explicit owner. Consumers use its API and event; no external writes to the schema are observed. Four independent families support a `Pricing` candidate, so it is `HIGH` confidence. It remains a candidate until governed.

### Structural cluster only

Packages under `customer` depend mostly on one another, but no ownership, data, contract, or meaningful vocabulary evidence exists. The engine reports an unresolved `customer` region and missing evidence. It does not infer a bounded context.

### Conflicted candidate

An `Origination` module has cohesive vocabulary and APIs, but another repository writes directly to its schema and both repositories share mutable domain entities. Support and counter-evidence are shown. The candidate may be `LOW`, split/refined, or unresolved according to the rule outcome; the conflicts are not averaged away.

### Intended mismatch

The accepted graph defines `Customer` and `Loan Application` contexts. Observed inference produces one combined region because of a cross-context shared model. The accepted contexts remain intended architecture. The observed combined region becomes comparison evidence and may generate drift findings; it never replaces the graph definitions.
