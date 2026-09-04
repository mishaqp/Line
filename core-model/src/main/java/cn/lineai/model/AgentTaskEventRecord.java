package cn.lineai.model;

public final class AgentTaskEventRecord {
    private final long id;
    private final String taskId;
    private final long sequence;
    private final String eventType;
    private final AgentTaskState state;
    private final String payloadJson;
    private final long createdAt;

    public AgentTaskEventRecord(
            long id,
            String taskId,
            long sequence,
            String eventType,
            AgentTaskState state,
            String payloadJson,
            long createdAt
    ) {
        this.id = id;
        this.taskId = taskId == null ? "" : taskId;
        this.sequence = sequence;
        this.eventType = eventType == null ? "" : eventType;
        this.state = state == null ? AgentTaskState.FAILED : state;
        this.payloadJson = payloadJson == null ? "" : payloadJson;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getTaskId() { return taskId; }
    public long getSequence() { return sequence; }
    public String getEventType() { return eventType; }
    public AgentTaskState getState() { return state; }
    public String getPayloadJson() { return payloadJson; }
    public long getCreatedAt() { return createdAt; }
}
