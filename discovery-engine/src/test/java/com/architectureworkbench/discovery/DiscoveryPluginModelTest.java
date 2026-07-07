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

class DiscoveryPluginModelTest {
    @TempDir
    Path tempDir;

    @Test
    void repositoryPluginDetectsRootReadmeDocsAdrSourceAndTestDirectories() throws IOException {
        createRepositoryShape(tempDir);

        DiscoveryPluginResult result = new RepositoryDiscoveryPlugin().discover(input(), executionContext());

        assertEquals(DiscoveryPluginStatus.SUCCEEDED, result.status());
        assertTrue(hasEvidence(result, "repository", "repository-root"));
        assertTrue(hasObservation(result, "readme-detected"));
        assertTrue(hasObservation(result, "docs-directory-detected"));
        assertTrue(hasObservation(result, "adr-directory-detected"));
        assertTrue(hasObservation(result, "source-directory-detected"));
        assertTrue(hasObservation(result, "test-directory-detected"));
        assertTrue(hasObservation(result, "build-file-detected"));
        assertTrue(hasObservation(result, "docker-file-detected"));
        assertTrue(hasObservation(result, "cicd-file-detected"));
    }

    @Test
    void singleModuleMavenProjectIsDetected() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>single</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.slf4j</groupId>
                      <artifactId>slf4j-api</artifactId>
                      <version>2.0.13</version>
                    </dependency>
                  </dependencies>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.13.0</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
                """);

        DiscoveryPluginResult result = new MavenDiscoveryPlugin().discover(input(), executionContext());

        assertEquals(DiscoveryPluginStatus.SUCCEEDED, result.status());
        assertTrue(hasEvidence(result, "build-file", "pom.xml"));
        assertTrue(hasEvidence(result, "build-module", "com.example:single:jar:1.0.0"));
        assertTrue(hasEvidence(result, "dependency-declaration", "org.slf4j:slf4j-api:2.0.13"));
        assertTrue(hasEvidence(result, "build-plugin", "org.apache.maven.plugins:maven-compiler-plugin:3.13.0"));
        assertTrue(hasObservation(result, "maven-module-detected"));
    }

    @Test
    void multiModuleMavenProjectIsDetected() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>app</module>
                    <module>domain</module>
                  </modules>
                </project>
                """);
        Files.createDirectories(tempDir.resolve("app"));
        Files.createDirectories(tempDir.resolve("domain"));
        Files.writeString(tempDir.resolve("app/pom.xml"), """
                <project>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                  </parent>
                  <artifactId>app</artifactId>
                </project>
                """);
        Files.writeString(tempDir.resolve("domain/pom.xml"), """
                <project>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                  </parent>
                  <artifactId>domain</artifactId>
                </project>
                """);

        DiscoveryPluginResult result = new MavenDiscoveryPlugin().discover(input(), executionContext());

        assertTrue(hasEvidence(result, "build-module-declaration", "app"));
        assertTrue(hasEvidence(result, "build-module-declaration", "domain"));
        assertTrue(hasEvidence(result, "maven-parent", "com.example:parent:1.0.0"));
        assertEquals(3, result.output().evidence().stream().filter(evidence -> evidence.evidenceType().equals("build-file")).count());
    }

    @Test
    void pluginResultIncludesConfidenceAndEvidenceProvenance() throws IOException {
        createRepositoryShape(tempDir);

        DiscoveryPluginResult result = new RepositoryDiscoveryPlugin().discover(input(), executionContext());

        DiscoveryEvidence evidence = result.output().evidence().stream()
                .filter(candidate -> candidate.evidenceType().equals("file"))
                .findFirst()
                .orElseThrow();
        assertTrue(evidence.confidence().value() > 0.0);
        assertFalse(evidence.confidence().rationale().isBlank());
        assertTrue(evidence.provenance().startsWith("path:"));
        assertFalse(evidence.references().isEmpty());
    }

    @Test
    void pluginEvidenceMapsIntoAimEvidenceAndObservation() throws IOException {
        createRepositoryShape(tempDir);
        DiscoveryPluginResult result = new RepositoryDiscoveryPlugin().discover(input(), executionContext());
        DiscoveryPluginAimMapper mapper = new DiscoveryPluginAimMapper();

        List<Evidence> evidence = result.output().evidence().stream().map(mapper::mapEvidence).toList();
        Observation observation = mapper.mapObservation(result.output().observations().get(0), evidence);

        assertFalse(evidence.isEmpty());
        assertTrue(evidence.get(0).source().startsWith("discovery-plugin:"));
        assertFalse(observation.relatedEvidence().isEmpty());
    }

    @Test
    void localRepositoryConnectorRemainsCompatibleWithLegacyArtifacts() throws IOException {
        createRepositoryShape(tempDir);

        DiscoveryResult result = new LocalRepositoryDiscoveryConnector().discover(new DiscoveryContext(
                DiscoveryRunId.newId(),
                new DiscoverySource("source", DiscoverySourceType.LOCAL_REPOSITORY, tempDir.toUri().toString(), "source"),
                tempDir,
                new com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph("graph"),
                "architect"
        ));

        assertTrue(result.artifacts().stream().anyMatch(artifact -> artifact.type() == DiscoveredArtifactType.POM_FILE));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> artifact.type() == DiscoveredArtifactType.README_DOC));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> artifact.type() == DiscoveredArtifactType.ADR_DIRECTORY));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> artifact.type() == DiscoveredArtifactType.TEST_DIRECTORY));
    }

    private DiscoveryInput input() {
        return DiscoveryInput.root(tempDir);
    }

    private DiscoveryExecutionContext executionContext() {
        return new DiscoveryExecutionContext(
                DiscoveryRunId.newId(),
                new DiscoverySource("source", DiscoverySourceType.LOCAL_REPOSITORY, tempDir.toUri().toString(), "source"),
                tempDir,
                "architect",
                "correlation"
        );
    }

    private static boolean hasEvidence(DiscoveryPluginResult result, String type, String identity) {
        return result.output().evidence().stream()
                .anyMatch(evidence -> evidence.evidenceType().equals(type) && evidence.identity().equals(identity));
    }

    private static boolean hasObservation(DiscoveryPluginResult result, String type) {
        return result.output().observations().stream().anyMatch(observation -> observation.observationType().equals(type));
    }

    private static void createRepositoryShape(Path root) throws IOException {
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                </project>
                """);
        Files.writeString(root.resolve("README.md"), "# Demo\n");
        Files.writeString(root.resolve("Dockerfile"), "FROM eclipse-temurin:21\n");
        Files.createDirectories(root.resolve(".github/workflows"));
        Files.writeString(root.resolve(".github/workflows/build.yml"), "name: build\n");
        Files.createDirectories(root.resolve("docs"));
        Files.writeString(root.resolve("docs/overview.md"), "# Overview\n");
        Files.createDirectories(root.resolve("architecture/adr"));
        Files.createDirectories(root.resolve("src/main/java/com/example"));
        Files.createDirectories(root.resolve("src/test/java/com/example"));
    }
}
