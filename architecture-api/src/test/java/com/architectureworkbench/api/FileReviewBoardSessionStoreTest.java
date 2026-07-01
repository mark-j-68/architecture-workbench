package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.ReviewBoardDecisionResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.workspace.WorkspaceId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileReviewBoardSessionStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void storesReviewBoardSessionSnapshotsUnderWorkspaceDirectory() {
        FileReviewBoardSessionStore store = new FileReviewBoardSessionStore(tempDir);
        WorkspaceId workspaceId = WorkspaceId.of("workspace-1");

        store.save(workspaceId, new ReviewBoardSessionResponse(
                "review-session-1",
                "workspace-1-graph",
                "correlation-1",
                "CLOSED",
                List.of("recommendation-1"),
                List.of("change-1"),
                List.of(),
                List.of(),
                new ReviewBoardDecisionResponse("ACCEPT_PROPOSED_CHANGE", "Approved.", List.of(), Instant.now())
        ));

        assertTrue(Files.exists(tempDir.resolve("workspace-1").resolve("review-board-sessions.json")));
        assertTrue(Files.exists(tempDir.resolve("workspace-1").resolve("manifest.json")));
    }
}
