package com.architectureworkbench.discovery;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts Java import evidence into deterministic package and module reference
 * evidence. It intentionally does not perform symbol or architectural analysis.
 */
public class PackageDependencyDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("package.dependency");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(
                ID,
                "Package Dependency Discovery Plugin",
                "0.2.2",
                "Dependency Plugin",
                List.of("java", "maven"),
                List.of(
                        DiscoveryPluginCapability.DETECT_PACKAGE_DEPENDENCIES,
                        DiscoveryPluginCapability.DETECT_MODULE_PACKAGE_REFERENCES,
                        DiscoveryPluginCapability.MAP_IMPORTS_TO_MAVEN_DEPENDENCIES
                ),
                List.of(
                        new DiscoveryPluginDependency(JavaStructureDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(MavenDiscoveryPlugin.ID, false)
                ),
                true
        );
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> priorEvidence = input.priorOutputs().stream()
                .flatMap(output -> output.evidence().stream())
                .toList();
        List<PackageLocation> packages = priorEvidence.stream()
                .filter(item -> item.evidenceType().equals("java-package"))
                .map(PackageLocation::from)
                .sorted(Comparator.comparingInt((PackageLocation location) -> location.name().length()).reversed()
                        .thenComparing(PackageLocation::name))
                .toList();
        List<MavenDependency> mavenDependencies = priorEvidence.stream()
                .filter(item -> item.evidenceType().equals("dependency-declaration"))
                .map(item -> MavenDependency.from(input.rootDirectory(), item))
                .filter(dependency -> !dependency.groupId().isBlank())
                .sorted(Comparator.comparingInt((MavenDependency dependency) -> dependency.groupId().length()).reversed()
                        .thenComparing(MavenDependency::coordinates))
                .toList();

        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        if (priorEvidence.stream().noneMatch(item -> item.source().equals(JavaStructureDiscoveryPlugin.ID.value()))) {
            diagnostics.add("No Java structure output was supplied; no package dependency evidence was produced.");
        }

        priorEvidence.stream()
                .filter(item -> item.evidenceType().equals("java-import"))
                .sorted(Comparator.comparing(item -> item.identity()))
                .forEach(importEvidence -> inspectImport(
                        importEvidence,
                        packages,
                        mavenDependencies,
                        evidence,
                        observations
                ));

        return DiscoveryPluginResult.succeeded(
                ID,
                new DiscoveryOutput(evidence, observations, diagnostics),
                Duration.between(started, Instant.now())
        );
    }

    private static void inspectImport(
            DiscoveryEvidence importEvidence,
            List<PackageLocation> packages,
            List<MavenDependency> mavenDependencies,
            List<DiscoveryEvidence> evidence,
            List<DiscoveryObservation> observations
    ) {
        String sourcePackage = importEvidence.attributes().getOrDefault("packageName", "");
        String importName = importEvidence.attributes().getOrDefault("importName", "");
        if (sourcePackage.isBlank() || importName.isBlank()) {
            return;
        }

        Optional<PackageLocation> internalTarget = packages.stream()
                .filter(location -> importMatchesPackage(importName, location.name()))
                .findFirst();
        String targetPackage = internalTarget.map(PackageLocation::name)
                .orElse(importEvidence.attributes().getOrDefault("importedPackage", ""));
        if (targetPackage.isBlank() || targetPackage.equals(sourcePackage)) {
            return;
        }

        String filePath = importEvidence.attributes().getOrDefault("filePath", firstReference(importEvidence));
        String line = importEvidence.attributes().getOrDefault("line", "");
        String sourceModule = importEvidence.attributes().getOrDefault("module", ".");
        String dependencyKind = internalTarget.isPresent() ? "internal" : "external";
        DiscoveryEvidence packageDependency = evidence(
                "package-dependency",
                filePath,
                sourcePackage + "->" + targetPackage + "@" + filePath + ":" + line,
                line,
                DiscoveryConfidence.high("Package dependency follows deterministically from an explicit Java import."),
                false,
                attributes(
                        "filePath", filePath,
                        "line", line,
                        "packageName", sourcePackage,
                        "className", importEvidence.attributes().getOrDefault("className", ""),
                        "sourcePackage", sourcePackage,
                        "targetPackage", targetPackage,
                        "importName", importName,
                        "dependencyKind", dependencyKind,
                        "sourceModule", sourceModule,
                        "targetModule", internalTarget.map(PackageLocation::module).orElse(""),
                        "supportingEvidenceId", importEvidence.evidenceId(),
                        "pluginId", ID.value()
                )
        );
        evidence.add(packageDependency);
        observations.add(observation(
                "package-imports-package",
                "Package " + sourcePackage + " imports package " + targetPackage + ".",
                packageDependency,
                List.of(importEvidence.evidenceId())
        ));

        internalTarget.filter(target -> !target.module().equals(sourceModule)).ifPresent(target -> {
            DiscoveryEvidence moduleReference = evidence(
                    "module-package-reference",
                    filePath,
                    sourceModule + "->" + target.module() + ":" + targetPackage + "@" + filePath + ":" + line,
                    line,
                    DiscoveryConfidence.high("Cross-module package reference follows from an import and observed module source roots."),
                    false,
                    attributes(
                            "filePath", filePath,
                            "line", line,
                            "packageName", sourcePackage,
                            "className", importEvidence.attributes().getOrDefault("className", ""),
                            "sourceModule", sourceModule,
                            "targetModule", target.module(),
                            "sourcePackage", sourcePackage,
                            "targetPackage", targetPackage,
                            "importName", importName,
                            "supportingEvidenceId", importEvidence.evidenceId(),
                            "pluginId", ID.value()
                    )
            );
            evidence.add(moduleReference);
            observations.add(observation(
                    "module-references-package",
                    "Module " + sourceModule + " references package " + targetPackage + " in module " + target.module() + ".",
                    moduleReference,
                    List.of(importEvidence.evidenceId(), target.evidenceId())
            ));
        });

        if (internalTarget.isEmpty()) {
            mavenDependencies.stream()
                    .filter(dependency -> dependency.module().equals(sourceModule))
                    .filter(dependency -> importMatchesGroup(importName, dependency.groupId()))
                    .findFirst()
                    .ifPresent(dependency -> {
                        DiscoveryEvidence externalReference = evidence(
                                "external-dependency-reference",
                                filePath,
                                sourcePackage + "->" + dependency.coordinates() + "@" + filePath + ":" + line,
                                line,
                                DiscoveryConfidence.inferred(0.85,
                                        "Import prefix matches an explicitly declared Maven dependency groupId."),
                                false,
                                attributes(
                                        "filePath", filePath,
                                        "line", line,
                                        "packageName", sourcePackage,
                                        "className", importEvidence.attributes().getOrDefault("className", ""),
                                        "sourceModule", sourceModule,
                                        "sourcePackage", sourcePackage,
                                        "targetPackage", targetPackage,
                                        "importName", importName,
                                        "mavenGroupId", dependency.groupId(),
                                        "mavenCoordinates", dependency.coordinates(),
                                        "dependencyEvidenceId", dependency.evidenceId(),
                                        "supportingEvidenceId", importEvidence.evidenceId(),
                                        "pluginId", ID.value()
                                )
                        );
                        evidence.add(externalReference);
                        observations.add(observation(
                                "package-references-maven-dependency",
                                "Package " + sourcePackage + " imports " + importName
                                        + ", matching Maven dependency " + dependency.coordinates() + ".",
                                externalReference,
                                List.of(importEvidence.evidenceId(), dependency.evidenceId())
                        ));
                    });
        }
    }

    private static boolean importMatchesPackage(String importName, String packageName) {
        return importName.equals(packageName) || importName.startsWith(packageName + ".");
    }

    private static boolean importMatchesGroup(String importName, String groupId) {
        return importName.equals(groupId) || importName.startsWith(groupId + ".");
    }

    private static DiscoveryEvidence evidence(
            String type,
            String filePath,
            String identity,
            String line,
            DiscoveryConfidence confidence,
            boolean directlyObserved,
            Map<String, String> attributes
    ) {
        return new DiscoveryEvidence(
                DiscoveryIdentity.stableId("dependency-evidence", type, identity),
                type,
                ID.value(),
                "path:" + filePath + (line.isBlank() ? "" : ":line:" + line),
                identity,
                confidence,
                directlyObserved,
                null,
                List.of(filePath),
                attributes
        );
    }

    private static DiscoveryObservation observation(
            String type,
            String description,
            DiscoveryEvidence evidence,
            List<String> supportingEvidenceIds
    ) {
        List<String> related = new ArrayList<>();
        related.add(evidence.evidenceId());
        related.addAll(supportingEvidenceIds);
        return new DiscoveryObservation(
                DiscoveryIdentity.stableId("dependency-observation", type, evidence.evidenceId()),
                type,
                description,
                evidence.confidence(),
                related,
                null
        );
    }

    private static String firstReference(DiscoveryEvidence evidence) {
        return evidence.references().isEmpty() ? "" : evidence.references().get(0);
    }

    private static Map<String, String> attributes(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index], values[index + 1]);
        }
        return Map.copyOf(result);
    }

    private record PackageLocation(String name, String module, String evidenceId) {
        static PackageLocation from(DiscoveryEvidence evidence) {
            return new PackageLocation(
                    evidence.attributes().getOrDefault("packageName", evidence.identity()),
                    evidence.attributes().getOrDefault("module", "."),
                    evidence.evidenceId()
            );
        }
    }

    private record MavenDependency(String groupId, String coordinates, String module, String evidenceId) {
        static MavenDependency from(Path root, DiscoveryEvidence evidence) {
            String reference = firstReference(evidence);
            Path pom = reference.isBlank() ? root.resolve("pom.xml") : root.resolve(reference).normalize();
            Path moduleDirectory = pom.getParent() == null ? root : pom.getParent();
            return new MavenDependency(
                    evidence.attributes().getOrDefault("groupId", ""),
                    evidence.attributes().getOrDefault("coordinates", evidence.identity()),
                    JavaStructureDiscoveryPlugin.relative(root, moduleDirectory),
                    evidence.evidenceId()
            );
        }
    }
}
