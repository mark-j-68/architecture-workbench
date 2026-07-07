package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RepositoryDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("repository.discovery");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(
                ID,
                "Repository Discovery Plugin",
                "0.2.1",
                "Source Plugin",
                List.of("filesystem", "repository"),
                List.of(
                        DiscoveryPluginCapability.DETECT_REPOSITORY_STRUCTURE,
                        DiscoveryPluginCapability.REGISTER_SOURCE
                ),
                List.of(),
                true
        );
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        try {
            if (!Files.isDirectory(input.rootDirectory())) {
                return DiscoveryPluginResult.failed(ID, Duration.between(started, Instant.now()), "Repository root is not a directory: " + input.rootDirectory());
            }
            List<DiscoveryEvidence> evidence = new ArrayList<>();
            List<DiscoveryObservation> observations = new ArrayList<>();
            List<String> markers = repositoryMarkers(input.rootDirectory());
            DiscoveryEvidence rootEvidence = evidence(
                    "repository",
                    input.rootDirectory(),
                    input.rootDirectory(),
                    "repository-root",
                    DiscoveryConfidence.inferred(rootConfidence(markers), "Repository root confidence based on markers: " + markers),
                    false,
                    Map.of("markers", String.join(",", markers))
            );
            evidence.add(rootEvidence);
            observations.add(observation("repository-root-detected", "Repository root detected at " + input.rootDirectory(), rootEvidence));

            try (Stream<Path> stream = Files.walk(input.rootDirectory())) {
                stream.forEach(path -> inspectPath(input.rootDirectory(), path, evidence, observations));
            }
            return DiscoveryPluginResult.succeeded(ID, new DiscoveryOutput(evidence, observations, List.of()), Duration.between(started, Instant.now()));
        } catch (IOException e) {
            return DiscoveryPluginResult.failed(ID, Duration.between(started, Instant.now()), "Unable to scan repository: " + e.getMessage());
        }
    }

    private static void inspectPath(Path root, Path path, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        if (path.equals(root)) {
            return;
        }
        String relative = relative(root, path);
        if (Files.isDirectory(path)) {
            addDirectoryEvidence(root, path, relative, evidence, observations);
        } else if (Files.isRegularFile(path)) {
            addFileEvidence(root, path, relative, evidence, observations);
        }
    }

    private static void addDirectoryEvidence(Path root, Path path, String relative, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        DiscoveryEvidence directory = evidence("directory", root, path, relative, DiscoveryConfidence.observedFact("Directory exists."), true, Map.of());
        evidence.add(directory);
        if (relative.equals("docs") || relative.startsWith("docs/")) {
            observations.add(observation("docs-directory-detected", "Documentation directory detected: " + relative, directory));
        }
        String lower = relative.toLowerCase();
        if (lower.equals("adr") || lower.endsWith("/adr") || lower.contains("/architecture/adr")) {
            observations.add(observation("adr-directory-detected", "ADR directory detected: " + relative, directory));
        }
        if (relative.endsWith("src/main") || relative.endsWith("src/main/java")) {
            observations.add(observation("source-directory-detected", "Source directory detected: " + relative, directory));
        }
        if (relative.endsWith("src/test") || relative.endsWith("src/test/java")) {
            observations.add(observation("test-directory-detected", "Test directory detected: " + relative, directory));
        }
    }

    private static void addFileEvidence(Path root, Path path, String relative, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        String fileName = path.getFileName().toString();
        DiscoveryEvidence file = evidence("file", root, path, relative, DiscoveryConfidence.observedFact("File exists."), true, Map.of("fileName", fileName));
        evidence.add(file);
        if (fileName.equalsIgnoreCase("README.md") || fileName.equalsIgnoreCase("README")) {
            observations.add(observation("readme-detected", "README file detected: " + relative, file));
        }
        if (isBuildFile(fileName)) {
            observations.add(observation("build-file-detected", "Build file detected: " + relative, file));
        }
        if (isDockerFile(fileName)) {
            observations.add(observation("docker-file-detected", "Docker file detected: " + relative, file));
        }
        if (isCiFile(relative)) {
            observations.add(observation("cicd-file-detected", "CI/CD file detected: " + relative, file));
        }
    }

    private static List<String> repositoryMarkers(Path root) {
        List<String> markers = new ArrayList<>();
        for (String marker : List.of(".git", "pom.xml", "build.gradle", "settings.gradle", "README.md", "src")) {
            if (Files.exists(root.resolve(marker))) {
                markers.add(marker);
            }
        }
        return markers;
    }

    private static double rootConfidence(List<String> markers) {
        return Math.min(0.95, 0.70 + (markers.size() * 0.05));
    }

    private static boolean isBuildFile(String fileName) {
        return fileName.equals("pom.xml")
                || fileName.equals("build.gradle")
                || fileName.equals("build.gradle.kts")
                || fileName.equals("settings.gradle")
                || fileName.equals("settings.gradle.kts");
    }

    private static boolean isDockerFile(String fileName) {
        return fileName.equals("Dockerfile") || fileName.startsWith("Dockerfile.") || fileName.startsWith("docker-compose");
    }

    private static boolean isCiFile(String relative) {
        return relative.startsWith(".github/workflows/")
                || relative.startsWith(".gitlab-ci")
                || relative.equals("Jenkinsfile")
                || relative.startsWith("azure-pipelines");
    }

    private static DiscoveryEvidence evidence(
            String type,
            Path root,
            Path path,
            String identity,
            DiscoveryConfidence confidence,
            boolean directlyObserved,
            Map<String, String> attributes
    ) {
        return new DiscoveryEvidence(
                null,
                type,
                ID.value(),
                "path:" + relative(root, path),
                identity,
                confidence,
                directlyObserved,
                null,
                List.of(relative(root, path)),
                attributes
        );
    }

    private static DiscoveryObservation observation(String type, String description, DiscoveryEvidence evidence) {
        return new DiscoveryObservation(null, type, description, evidence.confidence(), List.of(evidence.evidenceId()), null);
    }

    private static String relative(Path root, Path path) {
        if (path.equals(root)) {
            return ".";
        }
        return root.relativize(path).toString().replace('\\', '/');
    }
}
