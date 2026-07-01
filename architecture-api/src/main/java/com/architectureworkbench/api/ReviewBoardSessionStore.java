package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.workspace.WorkspaceId;

interface ReviewBoardSessionStore {
    ReviewBoardSessionResponse save(WorkspaceId workspaceId, ReviewBoardSessionResponse session);
}
