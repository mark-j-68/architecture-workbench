package com.architectureworkbench.workspace;

import com.architectureworkbench.audit.AuditEventRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FileWorkspaceIntegrityService {
    public static final String MANIFEST_FILE = "manifest.json";
    public static final List<String> MANIFEST_CHECKED_FILES = List.of(
            "workspace.json",
            "graph.json",
            "proposed-changes.json",
            "review-board-sessions.json",
            "audit-events.json"
    );

    private static final TypeReference<List<AuditEventRecord>> AUDIT_LIST = new TypeReference<>() {};
    private final Path root;

    public FileWorkspaceIntegrityService() {
        this(FileWorkspaceStorage.defaultRoot());
    }

    public FileWorkspaceIntegrityService(Path root) {
        this.root = root;
    }

    public synchronized StorageManifest refreshManifest(WorkspaceId workspaceId) {
        Path directory = FileWorkspaceStorage.workspaceDirectory(root, workspaceId);
        StorageManifest existing = readManifestIfPresent(directory);
        Instant now = Instant.now();
        StorageManifest manifest = new StorageManifest(
                workspaceId.value(),
                StorageManifest.CURRENT_SCHEMA_VERSION,
                existing == null ? now : existing.createdAt(),
                now,
                checksums(directory),
                lastAuditHash(directory.resolve("audit-events.json"))
        );
        WorkspaceJson.writeManifest(directory.resolve(MANIFEST_FILE), manifest);
        return manifest;
    }

    public synchronized WorkspaceIntegrityReport verifyWorkspace(WorkspaceId workspaceId) {
        Path directory = FileWorkspaceStorage.workspaceDirectory(root, workspaceId);
        List<String> failures = new ArrayList<>();
        Path manifestFile = directory.resolve(MANIFEST_FILE);
        if (!Files.exists(manifestFile)) {
            failures.add("Missing manifest: " + manifestFile);
            return new WorkspaceIntegrityReport(false, failures, "");
        }
        StorageManifest manifest = WorkspaceJson.read(manifestFile, StorageManifest.class);
        if (!workspaceId.value().equals(manifest.workspaceId())) {
            failures.add("Manifest workspace id mismatch: expected " + workspaceId.value() + " but found " + manifest.workspaceId());
        }
        if (manifest.schemaVersion() != StorageManifest.CURRENT_SCHEMA_VERSION) {
            failures.add("Unsupported manifest schema version: " + manifest.schemaVersion());
        }
        Map<String, String> expectedChecksums = new LinkedHashMap<>();
        manifest.checksums().forEach(checksum -> expectedChecksums.put(checksum.fileName(), checksum.sha256()));
        java.util.Set<String> checkedFiles = new java.util.LinkedHashSet<>(checkedFiles(directory));
        checkedFiles.addAll(expectedChecksums.keySet());
        for (String fileName : checkedFiles) {
            Path file = directory.resolve(fileName);
            String expected = expectedChecksums.get(fileName);
            if (!Files.exists(file)) {
                if (expected != null) {
                    failures.add("Manifest references missing file: " + fileName);
                }
                continue;
            }
            if (expected == null) {
                failures.add("Manifest missing checksum for file: " + fileName);
                continue;
            }
            String actual = sha256(file);
            if (!expected.equals(actual)) {
                failures.add("Checksum mismatch for file: " + fileName);
            }
        }
        Path auditFile = directory.resolve("audit-events.json");
        if (Files.exists(auditFile)) {
            verifyAuditChain(auditFile, failures);
            String lastHash = lastAuditHash(auditFile);
            if (!manifest.lastKnownAuditHash().equals(lastHash)) {
                failures.add("Manifest last audit hash mismatch.");
            }
        }
        return new WorkspaceIntegrityReport(failures.isEmpty(), failures, manifest.lastKnownAuditHash());
    }

    static void refreshManifestForJsonFile(Path file) {
        if (!file.getFileName().toString().equals(MANIFEST_FILE) && file.getParent() != null && file.getParent().getFileName() != null) {
            new FileWorkspaceIntegrityService(file.getParent().getParent()).refreshManifest(WorkspaceId.of(file.getParent().getFileName().toString()));
        }
    }

    static String sha256(Path path) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to checksum file: " + path, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required.", e);
        }
    }

    static String sha256Text(String value) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required.", e);
        }
    }

    private List<StorageFileChecksum> checksums(Path directory) {
        List<StorageFileChecksum> checksums = new ArrayList<>();
        for (String fileName : checkedFiles(directory)) {
            Path file = directory.resolve(fileName);
            if (Files.exists(file)) {
                checksums.add(new StorageFileChecksum(fileName, sha256(file)));
            }
        }
        return checksums;
    }

    private List<String> checkedFiles(Path directory) {
        List<String> files = new ArrayList<>(MANIFEST_CHECKED_FILES);
        Path discoveryRuns = directory.resolve("discovery-runs");
        if (Files.isDirectory(discoveryRuns)) {
            try (java.util.stream.Stream<Path> paths = Files.walk(discoveryRuns)) {
                paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(directory::relativize).map(path -> path.toString().replace('\\', '/')).sorted().forEach(files::add);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to enumerate discovery run files in " + directory, exception);
            }
        }
        return List.copyOf(files);
    }

    private StorageManifest readManifestIfPresent(Path directory) {
        Path manifestFile = directory.resolve(MANIFEST_FILE);
        return Files.exists(manifestFile) ? WorkspaceJson.read(manifestFile, StorageManifest.class) : null;
    }

    private static void verifyAuditChain(Path auditFile, List<String> failures) {
        List<AuditEventRecord> events = WorkspaceJson.read(auditFile, AUDIT_LIST);
        String previousHash = "GENESIS";
        for (AuditEventRecord event : events) {
            if (!previousHash.equals(event.previousHash())) {
                failures.add("Audit hash chain break at event: " + event.eventId());
                return;
            }
            previousHash = event.eventHash();
        }
    }

    private static String lastAuditHash(Path auditFile) {
        if (!Files.exists(auditFile)) {
            return "";
        }
        List<AuditEventRecord> events = WorkspaceJson.read(auditFile, AUDIT_LIST);
        return events.isEmpty() ? "" : events.get(events.size() - 1).eventHash();
    }

    private static String hex(byte[] digest) {
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
