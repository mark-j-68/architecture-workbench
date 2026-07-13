package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

public class LocalRepositoryDiscoveryConnector implements DiscoveryConnector {
    @Override
    public boolean supports(DiscoverySource source) {
        return source.type() == DiscoverySourceType.LOCAL_REPOSITORY;
    }

    @Override
    public DiscoveryResult discover(DiscoveryContext context) {
        if (!Files.isDirectory(context.rootDirectory())) {
            throw new IllegalArgumentException("Discovery root is not a directory: " + context.rootDirectory());
        }
        List<DiscoveredArtifact> artifacts = new ArrayList<>();
        DiscoveryExecutionContext executionContext = DiscoveryExecutionContext.from(context);
        DiscoveryPluginResult repositoryResult = new RepositoryDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()), executionContext);
        DiscoveryPluginResult mavenResult = new MavenDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(List.of(repositoryResult.output())),
                executionContext
        );
        DiscoveryPluginResult javaResult = new JavaStructureDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(List.of(repositoryResult.output(), mavenResult.output())),
                executionContext
        );
        DiscoveryPluginResult packageDependencyResult = new PackageDependencyDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(List.of(repositoryResult.output(), mavenResult.output(), javaResult.output())),
                executionContext
        );
        List<DiscoveryOutput> foundationalOutputs = List.of(repositoryResult.output(), mavenResult.output(), javaResult.output(), packageDependencyResult.output());
        List<DiscoveryPluginResult> springResults = List.of(
                new SpringApplicationDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(foundationalOutputs), executionContext),
                new SpringWebDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(foundationalOutputs), executionContext),
                new SpringComponentDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(foundationalOutputs), executionContext),
                new SpringDataDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(foundationalOutputs), executionContext),
                new SpringTransactionDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(foundationalOutputs), executionContext),
                new SpringMessagingDiscoveryPlugin().discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(foundationalOutputs), executionContext)
        );
        List<DiscoveryOutput> frameworkOutputs = new ArrayList<>(foundationalOutputs);
        frameworkOutputs.addAll(springResults.stream().map(DiscoveryPluginResult::output).toList());
        DiscoveryPluginResult openApiResult = new OpenApiContractDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(frameworkOutputs), executionContext);
        DiscoveryPluginResult eventResult = new EventContractDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(frameworkOutputs), executionContext);
        DiscoveryPluginResult commandResult = new CommandContractDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(frameworkOutputs), executionContext);
        List<DiscoveryOutput> contractOutputs = new ArrayList<>(frameworkOutputs);
        contractOutputs.addAll(List.of(openApiResult.output(), eventResult.output(), commandResult.output()));
        DiscoveryPluginResult topologyResult = new MessagingTopologyDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(contractOutputs), executionContext);
        DiscoveryPluginResult versionResult = new ContractVersionDiscoveryPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(contractOutputs), executionContext);
        DiscoveryPluginResult ownershipResult = new ContractOwnershipEvidencePlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(contractOutputs), executionContext);
        List<DiscoveryOutput> discoveryOutputs = new ArrayList<>(contractOutputs);
        discoveryOutputs.addAll(List.of(topologyResult.output(), versionResult.output(), ownershipResult.output()));
        DiscoveryPluginResult packageCycleAnalysis = new PackageCycleAnalysisPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(discoveryOutputs), executionContext);
        DiscoveryPluginResult moduleAnalysis = new ModuleDependencyAnalysisPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(discoveryOutputs), executionContext);
        DiscoveryPluginResult layerAnalysis = new LayerStructureAnalysisPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(discoveryOutputs), executionContext);
        DiscoveryPluginResult componentAnalysis = new ComponentDependencyAnalysisPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(discoveryOutputs), executionContext);
        DiscoveryPluginResult contractVersionAnalysis = new ContractVersionAnalysisPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(discoveryOutputs), executionContext);
        DiscoveryPluginResult messagingTopologyAnalysis = new MessagingTopologyAnalysisPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(discoveryOutputs), executionContext);
        List<DiscoveryOutput> metricInputs = new ArrayList<>(discoveryOutputs);
        metricInputs.addAll(List.of(packageCycleAnalysis.output(), moduleAnalysis.output(), layerAnalysis.output(),
                componentAnalysis.output(), contractVersionAnalysis.output(), messagingTopologyAnalysis.output()));
        DiscoveryPluginResult dependencyMetrics = new DependencyMetricsPlugin().discover(
                DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(metricInputs), executionContext);
        List<DiscoveryPluginResult> pluginResults = new ArrayList<>(List.of(repositoryResult, mavenResult, javaResult, packageDependencyResult));
        pluginResults.addAll(springResults);
        pluginResults.addAll(List.of(openApiResult, eventResult, commandResult, topologyResult, versionResult, ownershipResult));
        pluginResults.addAll(List.of(packageCycleAnalysis, moduleAnalysis, layerAnalysis, componentAnalysis,
                contractVersionAnalysis, messagingTopologyAnalysis, dependencyMetrics));
        pluginResults.forEach(result -> artifacts.addAll(toArtifacts(result)));

        try (Stream<Path> stream = Files.walk(context.rootDirectory())) {
            stream.filter(Files::isRegularFile).forEach(path -> inspectFile(context.rootDirectory(), path, artifacts));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan local repository: " + context.rootDirectory(), e);
        }
        discoverTestDirectories(context.rootDirectory(), artifacts);
        discoverAdrDirectories(context.rootDirectory(), artifacts);
        return new DiscoveryResult(context.runId(), context.source(), deduplicate(artifacts));
    }

    private static List<DiscoveredArtifact> toArtifacts(DiscoveryPluginResult result) {
        if (result.status() == DiscoveryPluginStatus.FAILED) {
            return List.of();
        }
        List<DiscoveredArtifact> artifacts = new ArrayList<>();
        for (DiscoveryEvidence evidence : result.output().evidence()) {
            switch (evidence.evidenceType()) {
                case "build-file" -> {
                    if (evidence.references().stream().anyMatch(reference -> reference.endsWith("pom.xml"))) {
                        artifacts.add(artifact(DiscoveredArtifactType.POM_FILE, evidence));
                    }
                }
                case "build-module-declaration" -> artifacts.add(new DiscoveredArtifact(
                        null,
                        DiscoveredArtifactType.MAVEN_MODULE,
                        evidence.attributes().getOrDefault("module", evidence.identity()),
                        evidence.attributes().getOrDefault("module", evidence.identity()),
                        metadata(evidence)
                ));
                case "java-package" -> artifacts.add(new DiscoveredArtifact(
                        null,
                        DiscoveredArtifactType.JAVA_PACKAGE,
                        evidence.attributes().getOrDefault("packageName", evidence.identity()),
                        evidence.attributes().getOrDefault("packageName", evidence.identity()),
                        metadata(evidence)
                ));
                case "java-class", "java-interface" -> {
                    String className = evidence.attributes().getOrDefault("className", "");
                    if (className.endsWith("Repository")) {
                        artifacts.add(artifact(DiscoveredArtifactType.REPOSITORY_CLASS, evidence));
                    }
                }
                case "spring-web-controller" -> artifacts.add(artifact(DiscoveredArtifactType.SPRING_CONTROLLER, evidence));
                case "spring-component" -> {
                    String kind = evidence.attributes().getOrDefault("componentKind", "");
                    if (kind.equals("service")) artifacts.add(artifact(DiscoveredArtifactType.SPRING_SERVICE, evidence));
                    else if (kind.equals("repository")) artifacts.add(artifact(DiscoveredArtifactType.REPOSITORY_CLASS, evidence));
                }
                case "spring-data-repository" -> artifacts.add(artifact(DiscoveredArtifactType.REPOSITORY_CLASS, evidence));
                case "spring-configuration-file" -> artifacts.add(artifact(DiscoveredArtifactType.CONFIGURATION_FILE, evidence));
                case "file" -> {
                    String fileName = evidence.attributes().getOrDefault("fileName", evidence.identity());
                    if (fileName.equalsIgnoreCase("README.md") || fileName.equalsIgnoreCase("README")) {
                        artifacts.add(artifact(DiscoveredArtifactType.README_DOC, evidence));
                    } else if (fileName.equals("Dockerfile") || fileName.startsWith("Dockerfile.") || fileName.startsWith("docker-compose")) {
                        artifacts.add(artifact(DiscoveredArtifactType.DOCKER_FILE, evidence));
                    }
                }
                case "directory" -> {
                    String identity = evidence.identity().toLowerCase();
                    if (identity.equals("adr") || identity.endsWith("/adr") || identity.contains("/architecture/adr")) {
                        artifacts.add(artifact(DiscoveredArtifactType.ADR_DIRECTORY, evidence));
                    } else if (identity.endsWith("src/test") || identity.endsWith("src/test/java")) {
                        artifacts.add(artifact(DiscoveredArtifactType.TEST_DIRECTORY, evidence));
                    }
                }
                default -> {
                    // Other plugin evidence is retained through plugin APIs; legacy artifacts only cover existing DTO types.
                }
            }
        }
        return artifacts;
    }

    private static DiscoveredArtifact artifact(DiscoveredArtifactType type, DiscoveryEvidence evidence) {
        String path = evidence.references().isEmpty() ? evidence.identity() : evidence.references().get(0);
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return new DiscoveredArtifact(null, type, path, name, metadata(evidence));
    }

    private static Map<String, String> metadata(DiscoveryEvidence evidence) {
        Map<String, String> metadata = new LinkedHashMap<>(evidence.attributes());
        metadata.put("pluginId", evidence.source());
        metadata.put("provenance", evidence.provenance());
        metadata.put("confidence", Double.toString(evidence.confidence().value()));
        metadata.put("confidenceRationale", evidence.confidence().rationale());
        return Map.copyOf(metadata);
    }

    private static List<DiscoveredArtifact> deduplicate(List<DiscoveredArtifact> artifacts) {
        Map<String, DiscoveredArtifact> unique = new LinkedHashMap<>();
        for (DiscoveredArtifact artifact : artifacts) {
            unique.putIfAbsent(artifact.type() + "|" + artifact.path() + "|" + artifact.name(), artifact);
        }
        return List.copyOf(unique.values());
    }

    private static void inspectFile(Path root, Path path, List<DiscoveredArtifact> artifacts) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String fileName = path.getFileName().toString();
        if (fileName.equals("pom.xml")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.POM_FILE, relative, fileName, Map.of()));
            discoverMavenModules(path, relative, artifacts);
        }
        if (fileName.equals("Dockerfile") || fileName.startsWith("Dockerfile.") || fileName.startsWith("docker-compose")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.DOCKER_FILE, relative, fileName, Map.of()));
        }
        if (fileName.equalsIgnoreCase("README.md") || fileName.equalsIgnoreCase("README")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.README_DOC, relative, fileName, Map.of()));
        } else if (relative.startsWith("docs/") && fileName.endsWith(".md")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.DOC_FILE, relative, fileName, Map.of()));
        }
        if (isConfigFile(fileName, relative)) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.CONFIGURATION_FILE, relative, fileName, Map.of()));
        }
    }

    private static void discoverMavenModules(Path pom, String relativePom, List<DiscoveredArtifact> artifacts) {
        try {
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
            var modules = document.getElementsByTagName("module");
            for (int i = 0; i < modules.getLength(); i++) {
                Element module = (Element) modules.item(i);
                String moduleName = module.getTextContent().trim();
                if (!moduleName.isBlank()) {
                    artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.MAVEN_MODULE, moduleName, moduleName, Map.of("declaredIn", relativePom)));
                }
            }
        } catch (Exception ignored) {
            // Invalid POMs are still useful as POM_FILE artefacts; deep Maven validation is out of scope.
        }
    }

    private static void discoverTestDirectories(Path root, List<DiscoveredArtifact> artifacts) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> root.relativize(path).toString().replace('\\', '/').endsWith("src/test"))
                    .forEach(path -> artifacts.add(new DiscoveredArtifact(
                            null,
                            DiscoveredArtifactType.TEST_DIRECTORY,
                            root.relativize(path).toString().replace('\\', '/'),
                            path.getFileName().toString(),
                            Map.of()
                    )));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan test directories.", e);
        }
    }

    private static void discoverAdrDirectories(Path root, List<DiscoveredArtifact> artifacts) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> {
                        String relative = root.relativize(path).toString().replace('\\', '/').toLowerCase();
                        return relative.equals("adr") || relative.endsWith("/adr") || relative.contains("/architecture/adr");
                    })
                    .forEach(path -> artifacts.add(new DiscoveredArtifact(
                            null,
                            DiscoveredArtifactType.ADR_DIRECTORY,
                            root.relativize(path).toString().replace('\\', '/'),
                            path.getFileName().toString(),
                            Map.of()
                    )));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan ADR directories.", e);
        }
    }

    private static boolean isConfigFile(String fileName, String relative) {
        return fileName.startsWith("application.") && (fileName.endsWith(".yml") || fileName.endsWith(".yaml") || fileName.endsWith(".properties"))
                || fileName.equals("application.yml")
                || fileName.equals("application.yaml")
                || relative.endsWith(".conf")
                || relative.endsWith(".config");
    }
}
