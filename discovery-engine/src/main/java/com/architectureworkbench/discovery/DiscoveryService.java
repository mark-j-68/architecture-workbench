package com.architectureworkbench.discovery;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditAppendRequest;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.DiscoveryCompleted;
import com.architectureworkbench.audit.DiscoveryStarted;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import com.architectureworkbench.knowledgegraph.ProposedChangeService;
import com.architectureworkbench.knowledgegraph.RelationshipService;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DiscoveryService {
    private final List<DiscoveryConnector> connectors;
    private final DiscoveryGraphMapper graphMapper;
    private final HealthcheckService healthcheckService;
    private final AuditSink auditSink;
    private final DiscoveredArtifactToEvidenceMapper evidenceMapper = new DiscoveredArtifactToEvidenceMapper();
    private final DiscoveryEvidenceToObservationMapper observationMapper = new DiscoveryEvidenceToObservationMapper();
    private final DiscoveryObservationToFindingMapper findingMapper = new DiscoveryObservationToFindingMapper();
    private final DiscoveryProposedChangeMapper proposedChangeMapper;

    public DiscoveryService(
            List<DiscoveryConnector> connectors,
            DiscoveryGraphMapper graphMapper,
            HealthcheckService healthcheckService,
            AuditSink auditSink
    ) {
        this(connectors, graphMapper, healthcheckService, auditSink, defaultProposedChangeService());
    }

    public DiscoveryService(
            List<DiscoveryConnector> connectors,
            DiscoveryGraphMapper graphMapper,
            HealthcheckService healthcheckService,
            AuditSink auditSink,
            ProposedChangeService proposedChangeService
    ) {
        this.connectors = List.copyOf(connectors);
        this.graphMapper = graphMapper;
        this.healthcheckService = healthcheckService;
        this.auditSink = auditSink;
        this.proposedChangeMapper = new DiscoveryProposedChangeMapper(proposedChangeService);
    }

    public DiscoveryRun runDiscovery(DiscoveryContext context) {
        Instant startedAt = Instant.now();
        CorrelationId correlationId = CorrelationId.newId("discovery");
        DiscoveryStarted started = new DiscoveryStarted(context.runId().value(), context.source().type().name(), context.source().sourceId());
        auditSink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.DISCOVERY_STARTED,
                context.graph().graphId(),
                ArchitectureEventSource.DISCOVERY_SERVICE,
                Actor.human(context.actorRef()),
                CausationId.newId("start-discovery"),
                correlationId,
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                started.payload(),
                null
        ));

        DiscoveryConnector connector = connectors.stream()
                .filter(candidate -> candidate.supports(context.source()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No discovery connector supports source type: " + context.source().type()));
        DiscoveryResult result = connector.discover(context);
        List<Evidence> artifactEvidence = result.artifacts().stream()
                .map(artifact -> evidenceMapper.map(context, artifact))
                .toList();
        List<Evidence> evidence = artifactEvidence.isEmpty() ? List.of(evidenceMapper.sourceEvidence(context)) : artifactEvidence;
        List<Observation> artifactObservations = result.artifacts().stream()
                .map(artifact -> observationMapper.map(artifact, evidence.stream()
                        .filter(candidate -> candidate.supportingArtifacts().contains(artifact.artifactId()))
                        .findFirst()
                        .orElseThrow()))
                .toList();
        List<Observation> observations = artifactObservations.isEmpty()
                ? List.of(new Observation(
                        "observation-" + context.runId().value(),
                        "discovery",
                        "Discovery run produced no artifacts; source evidence was recorded for healthcheck traceability.",
                        evidence,
                        List.of()))
                : artifactObservations;
        List<Finding> artifactFindings = result.artifacts().stream()
                .map(artifact -> findingMapper.mapArtifactObservation(artifact, observations.stream()
                        .filter(observation -> observation.id().equals("observation-" + artifact.artifactId()))
                        .findFirst()
                        .orElseThrow()))
                .toList();

        result.artifacts().forEach(artifact -> {
            auditSink.append(new AuditAppendRequest(
                    "DISCOVERY",
                    context.runId().value(),
                    context.actorRef(),
                    "DISCOVERY_ARTIFACT_DISCOVERED",
                    artifact.artifactId(),
                    Map.of("artifactType", artifact.type().name(), "path", artifact.path(), "name", artifact.name())
            ));
        });
        List<DiscoveryFinding> findings = healthcheckService.runHealthchecks(context, result);
        List<Finding> healthcheckFindings = healthcheckService.mapToAimFindings(findings, observations);
        List<Finding> aimFindings = java.util.stream.Stream.concat(artifactFindings.stream(), healthcheckFindings.stream()).toList();
        DiscoveryProposalResult proposalResult = proposedChangeMapper.proposeChanges(context, result.artifacts(), aimFindings, correlationId);

        Instant completedAt = Instant.now();
        DiscoveryCompleted completed = new DiscoveryCompleted(context.runId().value(), result.artifacts().size(), findings.size());
        auditSink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.DISCOVERY_COMPLETED,
                context.graph().graphId(),
                ArchitectureEventSource.DISCOVERY_SERVICE,
                Actor.human(context.actorRef()),
                CausationId.newId("complete-discovery"),
                correlationId,
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.BOTH,
                completed.payload(),
                null
        ));
        return new DiscoveryRun(
                context.runId(),
                context.source(),
                startedAt,
                completedAt,
                result.artifacts(),
                findings,
                evidence,
                observations,
                aimFindings,
                proposalResult.recommendations(),
                proposalResult.proposedChanges()
        );
    }

    private static ProposedChangeService defaultProposedChangeService() {
        ImmutableKnowledgeGraphAuditLog auditLog = new ImmutableKnowledgeGraphAuditLog();
        return new ProposedChangeService(new ArchitectureElementService(auditLog), new RelationshipService(auditLog));
    }
}
