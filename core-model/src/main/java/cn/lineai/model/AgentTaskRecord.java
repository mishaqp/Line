package cn.lineai.model;

public final class AgentTaskRecord {
    private final String id;
    private final String conversationId;
    private final String projectId;
    private final String workspacePath;
    private final String userMessageId;
    private final String prompt;
    private final String rawInputJson;
    private final String modelConfigId;
    private final AgentTaskState state;
    private final int generationId;
    private final int toolCallCount;
    private final int maxToolCalls;
    private final String checkpointJson;
    private final String lastError;
    private final int resumeCount;
    private final long createdAt;
    private final long updatedAt;
    private final long startedAt;
    private final long finishedAt;
    private final long deadlineAt;

    public AgentTaskRecord(
            String id,
            String conversationId,
            String projectId,
            String workspacePath,
            String userMessageId,
            String prompt,
            String rawInputJson,
            String modelConfigId,
            AgentTaskState state,
            int generationId,
            int toolCallCount,
            int maxToolCalls,
            String checkpointJson,
            String lastError,
            int resumeCount,
            long createdAt,
            long updatedAt,
            long startedAt,
            long finishedAt,
            long deadlineAt
    ) {
        this.id = safe(id);
        this.conversationId = safe(conversationId);
        this.projectId = safe(projectId);
        this.workspacePath = safe(workspacePath);
        this.userMessageId = safe(userMessageId);
        this.prompt = safe(prompt);
        this.rawInputJson = safe(rawInputJson);
        this.modelConfigId = safe(modelConfigId);
        this.state = state == null ? AgentTaskState.QUEUED : state;
        this.generationId = Math.max(0, generationId);
        this.toolCallCount = Math.max(0, toolCallCount);
        this.maxToolCalls = maxToolCalls < -1 ? -1 : maxToolCalls;
        this.checkpointJson = safe(checkpointJson);
        this.lastError = safe(lastError);
        this.resumeCount = Math.max(0, resumeCount);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.deadlineAt = deadlineAt;
    }

    public String getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getProjectId() { return projectId; }
    public String getWorkspacePath() { return workspacePath; }
    public String getUserMessageId() { return userMessageId; }
    public String getPrompt() { return prompt; }
    public String getRawInputJson() { return rawInputJson; }
    public String getModelConfigId() { return modelConfigId; }
    public AgentTaskState getState() { return state; }
    public int getGenerationId() { return generationId; }
    public int getToolCallCount() { return toolCallCount; }
    public int getMaxToolCalls() { return maxToolCalls; }
    public String getCheckpointJson() { return checkpointJson; }
    public String getLastError() { return lastError; }
    public int getResumeCount() { return resumeCount; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getStartedAt() { return startedAt; }
    public long getFinishedAt() { return finishedAt; }
    public long getDeadlineAt() { return deadlineAt; }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
