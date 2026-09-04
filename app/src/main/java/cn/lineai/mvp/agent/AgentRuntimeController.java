package cn.lineai.mvp.agent;

import cn.lineai.ai.ModelCancellationToken;
import cn.lineai.data.repository.AgentTaskRepository;
import cn.lineai.model.AgentTaskRecord;
import cn.lineai.model.AgentTaskState;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class AgentRuntimeController {
    public static final long TASK_BUDGET_MS = 30L * 60L * 1000L;
    private static final String DEADLINE_MESSAGE =
            "Agent Runtime достиг лимита времени в 30 минут и остановил задачу.";

    private final AgentTaskRepository repository;
    private final ScheduledExecutorService watchdog;
    private final Map<String, ScheduledFuture<?>> deadlines = new HashMap<>();
    private String activeTaskId = "";
    private ModelCancellationToken activeCancellationToken;

    public AgentRuntimeController(AgentTaskRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("AgentTaskRepository is required");
        }
        this.repository = repository;
        this.watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "linecode-agent-watchdog");
            thread.setDaemon(true);
            return thread;
        });
    }

    public int recoverInFlightTasks() {
        return repository.recoverInFlightTasks();
    }

    public synchronized AgentTaskRecord enqueue(
            String conversationId,
            String projectId,
            String workspacePath,
            String userMessageId,
            String prompt,
            String rawInputJson,
            String modelConfigId,
            int maxToolCalls
    ) {
        return repository.enqueue(
                conversationId, projectId, workspacePath, userMessageId,
                prompt, rawInputJson, modelConfigId, maxToolCalls
        );
    }

    public synchronized AgentTaskRecord startTask(
            String taskId,
            int generationId,
            ModelCancellationToken cancellationToken
    ) {
        AgentTaskRecord existing = repository.get(taskId);
        if (existing == null || existing.getState().isTerminal()) {
            return existing;
        }
        long now = System.currentTimeMillis();
        long deadlineAt = existing.getDeadlineAt() > now
                ? existing.getDeadlineAt()
                : now + TASK_BUDGET_MS;
        if (existing.getDeadlineAt() > 0 && existing.getDeadlineAt() <= now) {
            repository.finish(taskId, AgentTaskState.FAILED, DEADLINE_MESSAGE, "deadline_exceeded");
            return null;
        }
        AgentTaskRecord started = repository.start(taskId, generationId, deadlineAt);
        if (started == null || started.getState() != AgentTaskState.RUNNING) {
            return started;
        }
        cancelWatchdogLocked();
        activeTaskId = started.getId();
        activeCancellationToken = cancellationToken;
        long delay = Math.max(1L, deadlineAt - System.currentTimeMillis());
        deadlines.put(activeTaskId, watchdog.schedule(
                () -> expire(started.getId(), generationId, cancellationToken),
                delay,
                TimeUnit.MILLISECONDS
        ));
        return started;
    }

    public synchronized void checkpoint(
            int generationId,
            String phase,
            int toolCallCount,
            String payloadJson
    ) {
        AgentTaskRecord task = repository.getByGenerationId(generationId);
        if (task != null) {
            repository.checkpoint(task.getId(), generationId, phase, toolCallCount, payloadJson);
        }
    }

    public synchronized AgentTaskRecord finishGeneration(
            int generationId,
            AgentTaskState requestedState,
            String error
    ) {
        AgentTaskRecord current = repository.getByGenerationId(generationId);
        if (current == null) {
            return null;
        }
        AgentTaskState state = requestedState == null ? AgentTaskState.FAILED : requestedState;
        if (current.getState() == AgentTaskState.CANCELLED
                || current.getState() == AgentTaskState.PAUSED) {
            state = current.getState();
        }
        String message = error == null ? "" : error;
        if (isDeadlineExceeded(current)) {
            state = AgentTaskState.FAILED;
            message = DEADLINE_MESSAGE;
        }
        cancelWatchdogForTaskLocked(current.getId());
        if (current.getId().equals(activeTaskId)) {
            activeTaskId = "";
            activeCancellationToken = null;
        }
        return repository.finish(current.getId(), state, message, current.getCheckpointJson());
    }

    public synchronized AgentTaskRecord failTask(String taskId, String error) {
        AgentTaskRecord current = repository.get(taskId);
        if (current == null || current.getState().isTerminal()) {
            return current;
        }
        cancelWatchdogForTaskLocked(taskId);
        if (taskId.equals(activeTaskId)) {
            activeTaskId = "";
            activeCancellationToken = null;
        }
        return repository.finish(taskId, AgentTaskState.FAILED, error, current.getCheckpointJson());
    }

    public synchronized AgentTaskRecord cancelActiveTask(String reason) {
        String taskId = activeTaskId;
        if (taskId.length() == 0) {
            return null;
        }
        if (activeCancellationToken != null) {
            activeCancellationToken.cancel();
        }
        cancelWatchdogForTaskLocked(taskId);
        activeTaskId = "";
        activeCancellationToken = null;
        return repository.cancel(taskId, reason == null ? "Задача отменена пользователем." : reason);
    }

    public synchronized AgentTaskRecord pauseActiveTask(String reason) {
        String taskId = activeTaskId;
        if (taskId.length() == 0) {
            return null;
        }
        if (activeCancellationToken != null) {
            activeCancellationToken.cancel();
        }
        cancelWatchdogForTaskLocked(taskId);
        activeTaskId = "";
        activeCancellationToken = null;
        return repository.pause(taskId, reason == null ? "Задача поставлена на паузу." : reason);
    }

    public synchronized AgentTaskRecord findRecoverableTask(String conversationId) {
        return repository.getRecoverable(conversationId);
    }

    public synchronized AgentTaskRecord findNextQueuedTask(String conversationId) {
        return repository.getNextQueued(conversationId);
    }

    public synchronized AgentTaskRecord getTask(String taskId) {
        return repository.get(taskId);
    }

    public synchronized boolean resumeTask(String taskId, int generationId, ModelCancellationToken token) {
        AgentTaskRecord task = repository.get(taskId);
        if (task == null || !task.getState().canStart()) {
            return false;
        }
        return startTask(taskId, generationId, token) != null;
    }

    public synchronized int cancelTasksForConversation(String conversationId, String reason) {
        return repository.cancelForConversation(conversationId, reason);
    }

    public synchronized boolean isDeadlineExceeded(int generationId) {
        AgentTaskRecord task = repository.getByGenerationId(generationId);
        return task != null && isDeadlineExceeded(task);
    }

    public String deadlineMessage() {
        return DEADLINE_MESSAGE;
    }

    public synchronized String activeTaskId() {
        return activeTaskId;
    }

    public synchronized void shutdown() {
        for (ScheduledFuture<?> future : deadlines.values()) {
            if (future != null) {
                future.cancel(false);
            }
        }
        deadlines.clear();
        watchdog.shutdownNow();
        activeTaskId = "";
        activeCancellationToken = null;
    }

    private synchronized void expire(
            String taskId,
            int generationId,
            ModelCancellationToken cancellationToken
    ) {
        AgentTaskRecord current = repository.get(taskId);
        if (current == null || !taskId.equals(activeTaskId)
                || current.getState() != AgentTaskState.RUNNING
                || current.getGenerationId() != generationId) {
            return;
        }
        if (!isDeadlineExceeded(current)) {
            return;
        }
        repository.checkpoint(
                taskId,
                generationId,
                "deadline_exceeded",
                current.getToolCallCount(),
                "{\"reason\":\"deadline_exceeded\"}"
        );
        if (cancellationToken != null) {
            cancellationToken.cancel();
        }
    }

    private synchronized void cancelWatchdogLocked() {
        if (activeTaskId.length() == 0) {
            return;
        }
        cancelWatchdogForTaskLocked(activeTaskId);
    }

    private void cancelWatchdogForTaskLocked(String taskId) {
        ScheduledFuture<?> future = deadlines.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private boolean isDeadlineExceeded(AgentTaskRecord task) {
        return task != null && task.getDeadlineAt() > 0
                && System.currentTimeMillis() >= task.getDeadlineAt();
    }
}
