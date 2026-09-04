package cn.lineai.model;

public enum AgentTaskState {
    QUEUED("queued"),
    RUNNING("running"),
    PAUSED("paused"),
    RECOVERABLE("recoverable"),
    WAITING_FOR_APPROVAL("waiting_for_approval"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String storageValue;

    AgentTaskState(String storageValue) {
        this.storageValue = storageValue;
    }

    public String storageValue() {
        return storageValue;
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }

    public boolean isInFlight() {
        return this == RUNNING || this == WAITING_FOR_APPROVAL;
    }

    public boolean canStart() {
        return this == QUEUED || this == PAUSED || this == RECOVERABLE;
    }

    public static AgentTaskState fromStorage(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        for (AgentTaskState state : values()) {
            if (state.storageValue.equals(normalized)) {
                return state;
            }
        }
        return FAILED;
    }
}
