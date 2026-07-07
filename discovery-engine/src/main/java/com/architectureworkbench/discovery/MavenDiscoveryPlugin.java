package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MavenDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("maven.discovery");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(
                ID,
                "Maven Discovery Plugin",
                "0.2.1",
                "Build Plugin",
                List.of("maven", "java"),
                List.of(
                        DiscoveryPluginCapability.DETECT_MAVEN_BUILD,
                        DiscoveryPluginCapability.DETECT_BUILD_MODULES,
                        DiscoveryPluginCapability.DETECT_DEPENDENCIES,
                        DiscoveryPluginCapability.DETECT_BUILD_PLUGINS
                ),
                List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true)),
                true
        );
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        try {
            List<Path> poms;
            try (Stream<Path> stream = Files.walk(input.rootDirectory())) {
                poms = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equals("pom.xml"))
                        .sorted()
                        .toList();
            }
            List<DiscoveryEvidence> evidence = new ArrayList<>();
            List<DiscoveryObservation> observations = new ArrayList<>();
            for (Path pom : poms) {
                inspectPom(input.rootDirectory(), pom, evidence, observations);
            }
            return DiscoveryPluginResult.succeeded(ID, new DiscoveryOutput(evidence, observations, List.of()), Duration.between(started, Instant.now()));
        } catch (IOException e) {
            return DiscoveryPluginResult.failed(ID, Duration.between(started, Instant.now()), "Unable to scan Maven files: " + e.getMessage());
        }
    }

    private static void inspectPom(Path root, Path pom, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        DiscoveryEvidence pomEvidence = evidence(
                "build-file",
                root,
                pom,
                relative(root, pom),
                DiscoveryConfidence.observedFact("pom.xml file exists."),
                true,
                Map.of("buildSystem", "maven")
        );
        evidence.add(pomEvidence);
        observations.add(observation("maven-pom-detected", "Maven pom.xml detected: " + relative(root, pom), pomEvidence));

        Element project;
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder().parse(pom.toFile());
            project = document.getDocumentElement();
        } catch (Exception e) {
            observations.add(new DiscoveryObservation(
                    null,
                    "maven-pom-parse-failed",
                    "Unable to parse Maven pom.xml: " + relative(root, pom),
                    DiscoveryConfidence.inferred(0.2, "POM file exists but XML parsing failed."),
                    List.of(pomEvidence.evidenceId()),
                    null
            ));
            return;
        }

        Map<String, String> coordinates = coordinates(project);
        DiscoveryEvidence moduleEvidence = evidence(
                "build-module",
                root,
                pom,
                coordinates.get("coordinates"),
                DiscoveryConfidence.high("Maven coordinates parsed from pom.xml."),
                false,
                coordinates
        );
        evidence.add(moduleEvidence);
        observations.add(observation("maven-module-detected", "Maven module detected: " + coordinates.get("coordinates"), moduleEvidence));

        parent(project).ifPresent(parent -> {
            DiscoveryEvidence parentEvidence = evidence(
                    "maven-parent",
                    root,
                    pom,
                    parent.get("coordinates"),
                    DiscoveryConfidence.high("Maven parent coordinates parsed from pom.xml."),
                    false,
                    parent
            );
            evidence.add(parentEvidence);
            observations.add(observation("maven-parent-detected", "Maven parent detected: " + parent.get("coordinates"), parentEvidence));
        });

        for (String module : childText(project, "modules", "module")) {
            DiscoveryEvidence moduleDeclaration = evidence(
                    "build-module-declaration",
                    root,
                    pom,
                    module,
                    DiscoveryConfidence.high("Maven module declaration parsed from pom.xml."),
                    false,
                    Map.of("module", module, "declaredIn", relative(root, pom))
            );
            evidence.add(moduleDeclaration);
            observations.add(observation("maven-module-declaration-detected", "Maven module declaration detected: " + module, moduleDeclaration));
        }

        for (Map<String, String> dependency : childrenCoordinates(project, "dependencies", "dependency")) {
            DiscoveryEvidence dependencyEvidence = evidence(
                    "dependency-declaration",
                    root,
                    pom,
                    dependency.get("coordinates"),
                    DiscoveryConfidence.high("Maven dependency declaration parsed from pom.xml."),
                    false,
                    dependency
            );
            evidence.add(dependencyEvidence);
            observations.add(observation("maven-dependency-detected", "Maven dependency detected: " + dependency.get("coordinates"), dependencyEvidence));
        }

        for (Map<String, String> plugin : childrenCoordinates(child(project, "build"), "plugins", "plugin")) {
            DiscoveryEvidence pluginEvidence = evidence(
                    "build-plugin",
                    root,
                    pom,
                    plugin.get("coordinates"),
                    DiscoveryConfidence.high("Maven build plugin parsed from pom.xml."),
                    false,
                    plugin
            );
            evidence.add(pluginEvidence);
            observations.add(observation("maven-plugin-detected", "Maven plugin detected: " + plugin.get("coordinates"), pluginEvidence));
        }
    }

    private static Map<String, String> coordinates(Element project) {
        Map<String, String> parent = parent(project).orElse(Map.of());
        String groupId = directText(project, "groupId", parent.getOrDefault("groupId", ""));
        String artifactId = directText(project, "artifactId", "");
        String version = directText(project, "version", parent.getOrDefault("version", ""));
        String packaging = directText(project, "packaging", "jar");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("groupId", groupId);
        values.put("artifactId", artifactId);
        values.put("version", version);
        values.put("packaging", packaging);
        values.put("coordinates", "%s:%s:%s:%s".formatted(groupId, artifactId, packaging, version));
        return Map.copyOf(values);
    }

    private static java.util.Optional<Map<String, String>> parent(Element project) {
        Element parent = child(project, "parent");
        if (parent == null) {
            return java.util.Optional.empty();
        }
        String groupId = directText(parent, "groupId", "");
        String artifactId = directText(parent, "artifactId", "");
        String version = directText(parent, "version", "");
        if (groupId.isBlank() && artifactId.isBlank() && version.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Map.of(
                "groupId", groupId,
                "artifactId", artifactId,
                "version", version,
                "coordinates", "%s:%s:%s".formatted(groupId, artifactId, version)
        ));
    }

    private static List<Map<String, String>> childrenCoordinates(Element parent, String containerName, String childName) {
        if (parent == null) {
            return List.of();
        }
        Element container = child(parent, containerName);
        if (container == null) {
            return List.of();
        }
        List<Map<String, String>> values = new ArrayList<>();
        for (Element element : children(container, childName)) {
            String groupId = directText(element, "groupId", "");
            String artifactId = directText(element, "artifactId", "");
            String version = directText(element, "version", "");
            String scope = directText(element, "scope", "");
            String type = directText(element, "type", "");
            Map<String, String> coordinates = new LinkedHashMap<>();
            coordinates.put("groupId", groupId);
            coordinates.put("artifactId", artifactId);
            coordinates.put("version", version);
            coordinates.put("scope", scope);
            coordinates.put("type", type);
            coordinates.put("coordinates", "%s:%s:%s".formatted(groupId, artifactId, version));
            values.add(Map.copyOf(coordinates));
        }
        return values;
    }

    private static List<String> childText(Element parent, String containerName, String childName) {
        Element container = child(parent, containerName);
        if (container == null) {
            return List.of();
        }
        return children(container, childName).stream()
                .map(Element::getTextContent)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> children = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                children.add(element);
            }
        }
        return children;
    }

    private static String directText(Element parent, String name, String defaultValue) {
        Element child = child(parent, name);
        if (child == null) {
            return defaultValue;
        }
        String value = child.getTextContent().trim();
        return value.isBlank() ? defaultValue : value;
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
