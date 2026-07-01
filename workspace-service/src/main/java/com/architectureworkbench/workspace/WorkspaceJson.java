package com.architectureworkbench.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class WorkspaceJson {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    private WorkspaceJson() {
    }

    public static <T> T read(Path path, Class<T> type) {
        try {
            return MAPPER.readValue(path.toFile(), type);
        } catch (IOException e) {
            return recoverFromBackup(path, type, e);
        }
    }

    public static <T> T read(Path path, com.fasterxml.jackson.core.type.TypeReference<T> type) {
        try {
            return MAPPER.readValue(path.toFile(), type);
        } catch (IOException e) {
            return recoverFromBackup(path, type, e);
        }
    }

    public static void write(Path path, Object value) {
        writeJson(path, value, true);
    }

    static void writeManifest(Path path, Object value) {
        writeJson(path, value, false);
    }

    private static void writeJson(Path path, Object value, boolean refreshManifest) {
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                Files.copy(path, backupPath(path), StandardCopyOption.REPLACE_EXISTING);
            }
            Path tempFile = path.resolveSibling(path.getFileName() + ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), value);
            moveAtomicallyWherePractical(tempFile, path);
            if (refreshManifest) {
                FileWorkspaceIntegrityService.refreshManifestForJsonFile(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write JSON file: " + path, e);
        }
    }

    private static <T> T recoverFromBackup(Path path, Class<T> type, IOException original) {
        Path backup = backupPath(path);
        if (!Files.exists(backup)) {
            throw new IllegalStateException("Unable to read JSON file: " + path, original);
        }
        try {
            T recovered = MAPPER.readValue(backup.toFile(), type);
            Files.copy(backup, path, StandardCopyOption.REPLACE_EXISTING);
            refreshManifestAfterRecovery(path);
            return recovered;
        } catch (IOException backupFailure) {
            IllegalStateException exception = new IllegalStateException("Unable to read JSON file and backup is invalid: " + path, original);
            exception.addSuppressed(backupFailure);
            throw exception;
        }
    }

    private static <T> T recoverFromBackup(Path path, com.fasterxml.jackson.core.type.TypeReference<T> type, IOException original) {
        Path backup = backupPath(path);
        if (!Files.exists(backup)) {
            throw new IllegalStateException("Unable to read JSON file: " + path, original);
        }
        try {
            T recovered = MAPPER.readValue(backup.toFile(), type);
            Files.copy(backup, path, StandardCopyOption.REPLACE_EXISTING);
            refreshManifestAfterRecovery(path);
            return recovered;
        } catch (IOException backupFailure) {
            IllegalStateException exception = new IllegalStateException("Unable to read JSON file and backup is invalid: " + path, original);
            exception.addSuppressed(backupFailure);
            throw exception;
        }
    }

    private static void moveAtomicallyWherePractical(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicFailure) {
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackFailure) {
                UncheckedIOException exception = new UncheckedIOException("Unable to move temporary JSON file into place: " + target, fallbackFailure);
                exception.addSuppressed(atomicFailure);
                throw exception;
            }
        }
    }

    private static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private static void refreshManifestAfterRecovery(Path path) {
        if (!path.getFileName().toString().equals(FileWorkspaceIntegrityService.MANIFEST_FILE)) {
            FileWorkspaceIntegrityService.refreshManifestForJsonFile(path);
        }
    }
}
