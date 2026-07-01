package com.architectureworkbench.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FileWorkspaceRepository implements WorkspaceRepository {
    private final Path root;

    public FileWorkspaceRepository() {
        this(FileWorkspaceStorage.defaultRoot());
    }

    public FileWorkspaceRepository(Path root) {
        this.root = root;
    }

    @Override
    public synchronized Workspace save(Workspace workspace) {
        WorkspaceJson.write(workspaceFile(workspace.id()), workspace);
        return workspace;
    }

    @Override
    public synchronized Optional<Workspace> findById(WorkspaceId workspaceId) {
        Path path = workspaceFile(workspaceId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(WorkspaceJson.read(path, Workspace.class));
    }

    @Override
    public synchronized List<Workspace> findAll() {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(path -> path.resolve("workspace.json"))
                    .filter(Files::exists)
                    .map(path -> WorkspaceJson.read(path, Workspace.class))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list workspace storage directory: " + root, e);
        }
    }

    private Path workspaceFile(WorkspaceId workspaceId) {
        return FileWorkspaceStorage.workspaceDirectory(root, workspaceId).resolve("workspace.json");
    }
}
