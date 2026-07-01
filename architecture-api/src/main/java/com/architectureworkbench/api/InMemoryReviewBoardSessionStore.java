package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.workspace.WorkspaceId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryReviewBoardSessionStore implements ReviewBoardSessionStore {
    private final Map<String, ReviewBoardSessionResponse> sessions = new ConcurrentHashMap<>();

    @Override
    public ReviewBoardSessionResponse save(WorkspaceId workspaceId, ReviewBoardSessionResponse session) {
        sessions.put(session.sessionId(), session);
        return session;
    }
}
