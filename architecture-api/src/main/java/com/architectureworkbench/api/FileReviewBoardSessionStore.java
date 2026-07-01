package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.workspace.FileWorkspaceStorage;
import com.architectureworkbench.workspace.WorkspaceJson;
import com.architectureworkbench.workspace.WorkspaceId;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class FileReviewBoardSessionStore implements ReviewBoardSessionStore {
    private static final TypeReference<List<ReviewBoardSessionResponse>> SESSION_LIST = new TypeReference<>() {};

    private final Path root;

    FileReviewBoardSessionStore(Path root) {
        this.root = root;
    }

    @Override
    public synchronized ReviewBoardSessionResponse save(WorkspaceId workspaceId, ReviewBoardSessionResponse session) {
        Path file = sessionsFile(workspaceId);
        List<ReviewBoardSessionResponse> sessions = new ArrayList<>(readSessions(file));
        sessions.removeIf(candidate -> candidate.sessionId().equals(session.sessionId()));
        sessions.add(session);
        writeSessions(file, sessions);
        return session;
    }

    private List<ReviewBoardSessionResponse> readSessions(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        return WorkspaceJson.read(file, SESSION_LIST);
    }

    private void writeSessions(Path file, List<ReviewBoardSessionResponse> sessions) {
        WorkspaceJson.write(file, sessions);
    }

    private Path sessionsFile(WorkspaceId workspaceId) {
        return FileWorkspaceStorage.workspaceDirectory(root, workspaceId).resolve("review-board-sessions.json");
    }
}
