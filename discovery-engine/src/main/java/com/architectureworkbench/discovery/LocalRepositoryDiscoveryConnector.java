package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        Set<String> javaPackages = new HashSet<>();

        try (Stream<Path> stream = Files.walk(context.rootDirectory())) {
            stream.filter(Files::isRegularFile).forEach(path -> inspectFile(context.rootDirectory(), path, artifacts, javaPackages));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan local repository: " + context.rootDirectory(), e);
        }
        discoverTestDirectories(context.rootDirectory(), artifacts);
        discoverAdrDirectories(context.rootDirectory(), artifacts);
        javaPackages.stream().sorted().forEach(packageName -> artifacts.add(new DiscoveredArtifact(
                null,
                DiscoveredArtifactType.JAVA_PACKAGE,
                packageName,
                packageName,
                Map.of("source", "java-package")
        )));
        return new DiscoveryResult(context.runId(), context.source(), artifacts);
    }

    private static void inspectFile(Path root, Path path, List<DiscoveredArtifact> artifacts, Set<String> javaPackages) {
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
        if (fileName.endsWith(".java")) {
            inspectJavaFile(path, relative, artifacts, javaPackages);
        }
    }

    private static void inspectJavaFile(Path path, String relative, List<DiscoveredArtifact> artifacts, Set<String> javaPackages) {
        String content;
        try {
            content = Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Java file: " + path, e);
        }
        content.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("package "))
                .findFirst()
                .ifPresent(line -> javaPackages.add(line.replace("package ", "").replace(";", "").trim()));

        String fileName = path.getFileName().toString();
        if (content.contains("@RestController") || content.contains("@Controller")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.SPRING_CONTROLLER, relative, fileName, Map.of()));
        }
        if (content.contains("@Service")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.SPRING_SERVICE, relative, fileName, Map.of()));
        }
        if (content.contains("@Repository") || fileName.endsWith("Repository.java")) {
            artifacts.add(new DiscoveredArtifact(null, DiscoveredArtifactType.REPOSITORY_CLASS, relative, fileName, Map.of()));
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
