package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Observation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStructureAndPackageDependencyDiscoveryPluginTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsSourceRootsPackagesTypesAnnotationsImportsAndRelationships() throws IOException {
        createMultiModuleProject(tempDir);

        DiscoveryPluginResult javaResult = discoverJava();

        assertEquals(DiscoveryPluginStatus.SUCCEEDED, javaResult.status());
        assertTrue(hasEvidence(javaResult, "java-source-root", "app/src/main/java"));
        assertTrue(hasEvidence(javaResult, "java-test-source-root", "app/src/test/java"));
        assertTrue(hasAttribute(javaResult, "java-package", "packageName", "com.example.app"));
        assertTrue(hasAttribute(javaResult, "java-class", "qualifiedName", "com.example.app.LoanService"));
        assertTrue(hasAttribute(javaResult, "java-interface", "qualifiedName", "com.example.core.Port"));
        assertTrue(hasAttribute(javaResult, "java-enum", "qualifiedName", "com.example.core.LoanState"));
        assertTrue(hasAttribute(javaResult, "java-record", "qualifiedName", "com.example.core.LoanId"));
        assertTrue(hasAttribute(javaResult, "java-annotation-declaration", "qualifiedName", "com.example.core.DomainType"));
        assertTrue(hasAttribute(javaResult, "java-annotation", "annotationName", "Deprecated"));
        assertTrue(hasAttribute(javaResult, "java-import", "importName", "com.example.core.Port"));
        assertTrue(hasAttribute(javaResult, "java-inheritance", "targetType", "BaseService"));
        assertTrue(hasAttribute(javaResult, "java-implementation", "targetType", "Port"));
    }

    @Test
    void producesInternalExternalAndCrossModulePackageDependencyEvidence() throws IOException {
        createMultiModuleProject(tempDir);
        PluginRun run = runPlugins();

        DiscoveryPluginResult dependencies = new PackageDependencyDiscoveryPlugin().discover(
                input(run.repository(), run.maven(), run.java()), executionContext());

        assertEquals(DiscoveryPluginStatus.SUCCEEDED, dependencies.status());
        assertTrue(hasDependency(dependencies, "package-dependency", "com.example.app", "com.example.core"));
        assertTrue(hasAttribute(dependencies, "package-dependency", "dependencyKind", "internal"));
        assertTrue(hasAttribute(dependencies, "module-package-reference", "sourceModule", "app"));
        assertTrue(hasAttribute(dependencies, "module-package-reference", "targetModule", "core"));
        assertTrue(hasAttribute(dependencies, "external-dependency-reference", "mavenGroupId", "org.slf4j"));
        assertTrue(hasObservation(dependencies, "package-imports-package"));
        assertTrue(hasObservation(dependencies, "module-references-package"));
        assertTrue(hasObservation(dependencies, "package-references-maven-dependency"));
    }

    @Test
    void handlesRepositoryWithoutJavaSources() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), minimalPom("empty", ""));

        DiscoveryPluginResult javaResult = discoverJava();
        DiscoveryPluginResult dependencies = new PackageDependencyDiscoveryPlugin().discover(
                input(javaResult), executionContext());

        assertEquals(DiscoveryPluginStatus.SUCCEEDED, javaResult.status());
        assertTrue(javaResult.output().evidence().isEmpty());
        assertEquals(DiscoveryPluginStatus.SUCCEEDED, dependencies.status());
        assertTrue(dependencies.output().evidence().isEmpty());
    }

    @Test
    void extractsPartialEvidenceFromSyntacticallyImperfectJava() throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java/com/example/broken"));
        Files.writeString(tempDir.resolve("src/main/java/com/example/broken/Broken.java"), """
                package com.example.broken;
                import java.util.List;
                public class Broken implements Runnable {
                    private List<String> values;
                """);

        DiscoveryPluginResult result = discoverJava();

        assertEquals(DiscoveryPluginStatus.SUCCEEDED, result.status());
        assertTrue(hasAttribute(result, "java-package", "packageName", "com.example.broken"));
        assertTrue(hasAttribute(result, "java-class", "qualifiedName", "com.example.broken.Broken"));
        assertTrue(hasAttribute(result, "java-import", "importName", "java.util.List"));
        assertTrue(hasAttribute(result, "java-implementation", "targetType", "Runnable"));
    }

    @Test
    void evidenceHasStableIdentityConfidenceAndCompleteProvenance() throws IOException {
        createMultiModuleProject(tempDir);

        DiscoveryPluginResult first = discoverJava();
        DiscoveryPluginResult second = discoverJava();
        DiscoveryEvidence type = first.output().evidence().stream()
                .filter(item -> item.evidenceType().equals("java-class"))
                .findFirst()
                .orElseThrow();

        assertTrue(type.confidence().value() > 0.0);
        assertFalse(type.confidence().rationale().isBlank());
        assertEquals(JavaStructureDiscoveryPlugin.ID.value(), type.source());
        assertEquals(JavaStructureDiscoveryPlugin.ID.value(), type.attributes().get("pluginId"));
        assertFalse(type.attributes().get("filePath").isBlank());
        assertFalse(type.attributes().get("packageName").isBlank());
        assertFalse(type.attributes().get("className").isBlank());
        assertFalse(type.attributes().get("line").isBlank());
        assertTrue(type.provenance().contains(":line:"));
        assertEquals(
                first.output().evidence().stream().map(DiscoveryEvidence::evidenceId).toList(),
                second.output().evidence().stream().map(DiscoveryEvidence::evidenceId).toList()
        );
    }

    @Test
    void dependencyEvidenceAndObservationsMapIntoAim() throws IOException {
        createMultiModuleProject(tempDir);
        PluginRun run = runPlugins();
        DiscoveryPluginResult dependencyResult = new PackageDependencyDiscoveryPlugin().discover(
                input(run.repository(), run.maven(), run.java()), executionContext());
        DiscoveryPluginAimMapper mapper = new DiscoveryPluginAimMapper();

        List<Evidence> mappedEvidence = dependencyResult.output().evidence().stream()
                .map(mapper::mapEvidence)
                .toList();
        DiscoveryObservation packageObservation = dependencyResult.output().observations().stream()
                .filter(item -> item.observationType().equals("package-imports-package"))
                .findFirst()
                .orElseThrow();
        Observation mappedObservation = mapper.mapObservation(packageObservation, mappedEvidence);

        assertFalse(mappedEvidence.isEmpty());
        assertTrue(mappedEvidence.stream().allMatch(item -> item.source().startsWith("discovery-plugin:")));
        assertFalse(mappedObservation.relatedEvidence().isEmpty());
    }

    @Test
    void localConnectorUsesJavaPluginEvidenceForLegacyPackageArtifacts() throws IOException {
        createMultiModuleProject(tempDir);

        DiscoveryResult result = new LocalRepositoryDiscoveryConnector().discover(new DiscoveryContext(
                DiscoveryRunId.newId(),
                source(),
                tempDir,
                new com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph("graph"),
                "architect"
        ));

        assertTrue(result.artifacts().stream()
                .filter(item -> item.type() == DiscoveredArtifactType.JAVA_PACKAGE)
                .anyMatch(item -> item.name().equals("com.example.app")
                        && item.metadata().get("pluginId").equals(JavaStructureDiscoveryPlugin.ID.value())));
    }

    private PluginRun runPlugins() {
        DiscoveryPluginResult repository = new RepositoryDiscoveryPlugin().discover(input(), executionContext());
        DiscoveryPluginResult maven = new MavenDiscoveryPlugin().discover(input(repository), executionContext());
        DiscoveryPluginResult java = new JavaStructureDiscoveryPlugin().discover(input(repository, maven), executionContext());
        return new PluginRun(repository, maven, java);
    }

    private DiscoveryPluginResult discoverJava() {
        DiscoveryPluginResult repository = new RepositoryDiscoveryPlugin().discover(input(), executionContext());
        DiscoveryPluginResult maven = new MavenDiscoveryPlugin().discover(input(repository), executionContext());
        return new JavaStructureDiscoveryPlugin().discover(input(repository, maven), executionContext());
    }

    private DiscoveryInput input(DiscoveryPluginResult... results) {
        return DiscoveryInput.root(tempDir).withPriorOutputs(
                java.util.Arrays.stream(results).map(DiscoveryPluginResult::output).toList());
    }

    private DiscoveryExecutionContext executionContext() {
        return new DiscoveryExecutionContext(
                DiscoveryRunId.newId(),
                source(),
                tempDir,
                "architect",
                "correlation"
        );
    }

    private DiscoverySource source() {
        return new DiscoverySource("source", DiscoverySourceType.LOCAL_REPOSITORY, tempDir.toUri().toString(), "source");
    }

    private static boolean hasEvidence(DiscoveryPluginResult result, String type, String reference) {
        return result.output().evidence().stream()
                .anyMatch(item -> item.evidenceType().equals(type) && item.references().contains(reference));
    }

    private static boolean hasAttribute(DiscoveryPluginResult result, String type, String name, String value) {
        return result.output().evidence().stream()
                .anyMatch(item -> item.evidenceType().equals(type) && item.attributes().getOrDefault(name, "").equals(value));
    }

    private static boolean hasDependency(DiscoveryPluginResult result, String type, String source, String target) {
        return result.output().evidence().stream().anyMatch(item -> item.evidenceType().equals(type)
                && item.attributes().getOrDefault("sourcePackage", "").equals(source)
                && item.attributes().getOrDefault("targetPackage", "").equals(target));
    }

    private static boolean hasObservation(DiscoveryPluginResult result, String type) {
        return result.output().observations().stream().anyMatch(item -> item.observationType().equals(type));
    }

    private static void createMultiModuleProject(Path root) throws IOException {
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId><artifactId>parent</artifactId><version>1.0</version>
                  <packaging>pom</packaging>
                  <modules><module>app</module><module>core</module></modules>
                </project>
                """);
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app"));
        Files.createDirectories(root.resolve("app/src/test/java/com/example/app"));
        Files.createDirectories(root.resolve("core/src/main/java/com/example/core"));
        Files.writeString(root.resolve("app/pom.xml"), minimalPom("app", """
                <dependencies>
                  <dependency><groupId>com.example</groupId><artifactId>core</artifactId><version>1.0</version></dependency>
                  <dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId><version>2.0.13</version></dependency>
                </dependencies>
                """));
        Files.writeString(root.resolve("core/pom.xml"), minimalPom("core", ""));
        Files.writeString(root.resolve("core/src/main/java/com/example/core/Port.java"), """
                package com.example.core;
                public interface Port {}
                """);
        Files.writeString(root.resolve("core/src/main/java/com/example/core/LoanState.java"), """
                package com.example.core;
                public enum LoanState { OPEN, CLOSED }
                """);
        Files.writeString(root.resolve("core/src/main/java/com/example/core/LoanId.java"), """
                package com.example.core;
                public record LoanId(String value) {}
                """);
        Files.writeString(root.resolve("core/src/main/java/com/example/core/DomainType.java"), """
                package com.example.core;
                public @interface DomainType {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/LoanService.java"), """
                package com.example.app;

                import com.example.core.Port;
                import org.slf4j.Logger;

                @Deprecated
                public class LoanService extends BaseService implements Port {
                    private Logger logger;
                }
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/BaseService.java"), """
                package com.example.app;
                public class BaseService {}
                """);
        Files.writeString(root.resolve("app/src/test/java/com/example/app/LoanServiceTest.java"), """
                package com.example.app;
                class LoanServiceTest {}
                """);
    }

    private static String minimalPom(String artifactId, String body) {
        return """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent><groupId>com.example</groupId><artifactId>parent</artifactId><version>1.0</version></parent>
                  <artifactId>%s</artifactId>
                  %s
                </project>
                """.formatted(artifactId, body);
    }

    private record PluginRun(
            DiscoveryPluginResult repository,
            DiscoveryPluginResult maven,
            DiscoveryPluginResult java
    ) {
    }
}
