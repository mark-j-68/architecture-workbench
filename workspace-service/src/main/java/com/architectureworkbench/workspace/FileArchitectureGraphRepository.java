package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileArchitectureGraphRepository implements ArchitectureGraphRepository {
    private final Path root;
    private final GraphSnapshotMapper snapshotMapper = new GraphSnapshotMapper();

    public FileArchitectureGraphRepository() {
        this(FileWorkspaceStorage.defaultRoot());
    }

    public FileArchitectureGraphRepository(Path root) {
        this.root = root;
    }

    @Override
    public synchronized ArchitectureKnowledgeGraph save(WorkspaceId workspaceId, ArchitectureKnowledgeGraph graph) {
        WorkspaceJson.write(graphFile(workspaceId), snapshotMapper.exportSnapshot(graph));
        return graph;
    }

    @Override
    public synchronized Optional<ArchitectureKnowledgeGraph> findByWorkspaceId(WorkspaceId workspaceId) {
        Path path = graphFile(workspaceId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(snapshotMapper.importSnapshot(WorkspaceJson.read(path, GraphSnapshot.class)));
    }

    private Path graphFile(WorkspaceId workspaceId) {
        return FileWorkspaceStorage.workspaceDirectory(root, workspaceId).resolve("graph.json");
    }
}
