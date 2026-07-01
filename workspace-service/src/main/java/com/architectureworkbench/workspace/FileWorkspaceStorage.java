package com.architectureworkbench.workspace;

import java.nio.file.Path;
import java.util.Objects;

public final class FileWorkspaceStorage {
    public static final String STORAGE_DIR_PROPERTY = "architecture.workbench.storage.dir";
    public static final String STORAGE_DIR_ENV = "ARCHITECTURE_WORKBENCH_STORAGE_DIR";
    private static final Path DEFAULT_ROOT = Path.of("data", "workspaces");

    private FileWorkspaceStorage() {
    }

    public static Path defaultRoot() {
        String propertyValue = System.getProperty(STORAGE_DIR_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Path.of(propertyValue);
        }
        String environmentValue = System.getenv(STORAGE_DIR_ENV);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Path.of(environmentValue);
        }
        return DEFAULT_ROOT;
    }

    public static Path workspaceDirectory(Path root, WorkspaceId workspaceId) {
        return Objects.requireNonNull(root, "root").resolve(Objects.requireNonNull(workspaceId, "workspaceId").value());
    }
}
