package com.architectureworkbench.discovery;

import com.architectureworkbench.audit.AuditAppendRequest;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Observation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HealthcheckService {
    private final AuditSink auditSink;

    public HealthcheckService(AuditSink auditSink) {
        this.auditSink = auditSink;
    }

    public List<DiscoveryFinding> runHealthchecks(DiscoveryContext context, DiscoveryResult result) {
        List<DiscoveredArtifact> artifacts = result.artifacts();
        List<DiscoveryFinding> findings = new ArrayList<>();

        if (none(artifacts, DiscoveredArtifactType.README_DOC)) {
            findings.add(finding(DiscoveryFindingSeverity.WARNING, "DISC-README-001", "Repository has no obvious README.", ""));
        }
        if (none(artifacts, DiscoveredArtifactType.TEST_DIRECTORY)) {
            findings.add(finding(DiscoveryFindingSeverity.WARNING, "DISC-TEST-001", "Repository has no obvious test directory.", ""));
        }
        if (none(artifacts, DiscoveredArtifactType.ADR_DIRECTORY)) {
            findings.add(finding(DiscoveryFindingSeverity.WARNING, "DISC-ADR-001", "Repository has no ADR directory.", ""));
        }
        long moduleCount = count(artifacts, DiscoveredArtifactType.MAVEN_MODULE);
        if (moduleCount > 1) {
            findings.add(finding(DiscoveryFindingSeverity.INFO, "DISC-MODULE-001", "Multiple Maven modules detected: " + moduleCount, ""));
        }
        if (any(artifacts, DiscoveredArtifactType.SPRING_CONTROLLER)) {
            findings.add(finding(DiscoveryFindingSeverity.INFO, "DISC-SPRING-CTRL-001", "Spring controllers detected.", ""));
        }
        if (any(artifacts, DiscoveredArtifactType.DOCKER_FILE)) {
            findings.add(finding(DiscoveryFindingSeverity.INFO, "DISC-DOCKER-001", "Dockerfile or docker-compose file detected.", ""));
        }
        if (none(artifacts, DiscoveredArtifactType.SPRING_CONTROLLER)) {
            findings.add(finding(DiscoveryFindingSeverity.WARNING, "DISC-API-001", "No obvious API layer detected.", ""));
        }
        if (artifacts.stream().noneMatch(artifact -> artifact.type() == DiscoveredArtifactType.JAVA_PACKAGE && artifact.name().toLowerCase().contains("domain"))) {
            findings.add(finding(DiscoveryFindingSeverity.WARNING, "DISC-DOMAIN-001", "No obvious domain layer detected.", ""));
        }

        findings.forEach(finding -> auditSink.append(new AuditAppendRequest(
                "DISCOVERY",
                context.runId().value(),
                context.actorRef(),
                "HEALTHCHECK_FINDING_CREATED",
                finding.findingId(),
                Map.of("ruleId", finding.ruleId(), "severity", finding.severity().name(), "message", finding.message())
        )));
        return List.copyOf(findings);
    }

    public List<Finding> mapToAimFindings(List<DiscoveryFinding> findings, List<Observation> observations) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("Healthcheck AIM findings require observations.");
        }
        DiscoveryObservationToFindingMapper mapper = new DiscoveryObservationToFindingMapper();
        return findings.stream()
                .map(finding -> mapper.mapHealthcheckFinding(finding, observationsFor(finding, observations)))
                .toList();
    }

    private static DiscoveryFinding finding(DiscoveryFindingSeverity severity, String ruleId, String message, String artifactRef) {
        return new DiscoveryFinding(null, severity, ruleId, message, artifactRef);
    }

    private static List<Observation> observationsFor(DiscoveryFinding finding, List<Observation> observations) {
        if (!finding.artifactRef().isBlank()) {
            List<Observation> related = observations.stream()
                    .filter(observation -> observation.relatedEvidence().stream()
                            .flatMap(evidence -> evidence.supportingArtifacts().stream())
                            .anyMatch(ref -> ref.equals(finding.artifactRef())))
                    .toList();
            if (!related.isEmpty()) {
                return related;
            }
        }
        return observations;
    }

    private static boolean any(List<DiscoveredArtifact> artifacts, DiscoveredArtifactType type) {
        return artifacts.stream().anyMatch(artifact -> artifact.type() == type);
    }

    private static boolean none(List<DiscoveredArtifact> artifacts, DiscoveredArtifactType type) {
        return !any(artifacts, type);
    }

    private static long count(List<DiscoveredArtifact> artifacts, DiscoveredArtifactType type) {
        return artifacts.stream().filter(artifact -> artifact.type() == type).count();
    }
}
